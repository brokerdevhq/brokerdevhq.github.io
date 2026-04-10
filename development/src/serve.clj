
(ns serve
  (:require [ca.brokerdev.site-generator.interface :as generator]
            [org.httpkit.server :as http]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.file Files
            Path
            Paths
            FileSystems
            WatchService
            WatchKey
            StandardWatchEventKinds
            ClosedWatchServiceException
            FileVisitOption
            LinkOption]
           [java.util.concurrent Executors
            ExecutorService
            TimeUnit]))

(def site-config
  {:site-title "BrokerDev"
   :css ["/css/style.css"]})

(defonce server (atom nil))
(defonce watcher (atom nil))
(defonce watcher-executor (atom nil))
(defonce build-lock (Object.))

(def watch-dirs
  ["content" "static"])

(def debounce-ms 250)

(defn- mime-type [path]
  (cond
    (str/ends-with? path ".html") "text/html; charset=utf-8"
    (str/ends-with? path ".css")  "text/css; charset=utf-8"
    (str/ends-with? path ".js")   "application/javascript; charset=utf-8"
    (str/ends-with? path ".png")  "image/png"
    (str/ends-with? path ".jpg")  "image/jpeg"
    (str/ends-with? path ".jpeg") "image/jpeg"
    (str/ends-with? path ".svg")  "image/svg+xml; charset=utf-8"
    (str/ends-with? path ".ico")  "image/x-icon"
    :else                         "application/octet-stream"))

(defn- html-response [file]
  {:status  200
   :headers {"Content-Type"  "text/html; charset=utf-8"
             "Cache-Control" "no-cache, no-store, must-revalidate"}
   :body    (slurp file)})

(defn- binary-response [file path]
  {:status  200
   :headers {"Content-Type"  (mime-type path)
             "Cache-Control" "no-cache, no-store, must-revalidate"}
   :body    (Files/readAllBytes (.toPath file))})

(defn- handler [req]
  (let [uri  (if (= "/" (:uri req)) "/index.html" (:uri req))
        path (str "public" uri)
        file (io/file path)]
    (if (.exists file)
      (if (str/ends-with? path ".html")
        (html-response file)
        (binary-response file path))
      {:status  404
       :headers {"Content-Type" "text/plain; charset=utf-8"}
       :body    (str "Not found: " uri)})))

(defn generate! []
  (locking build-lock
    (generator/generate-site
     {:content-dir "content"
      :output-dir  "public"
      :static-dir  "static"
      :site-config site-config})))

(defn- register-dir! [^WatchService watch-service ^Path dir]
  (.register dir
             watch-service
             (into-array
              [StandardWatchEventKinds/ENTRY_CREATE
               StandardWatchEventKinds/ENTRY_MODIFY
               StandardWatchEventKinds/ENTRY_DELETE])))

(defn- register-dir-tree! [^WatchService watch-service root-dir]
  (let [^Path root (Paths/get root-dir (make-array String 0))]
    (when (Files/exists root (make-array LinkOption 0))
      (with-open [stream (Files/walk root (make-array java.nio.file.FileVisitOption 0))]
        (doseq [^Path path (iterator-seq (.iterator stream))]
          (when (Files/isDirectory path (make-array LinkOption 0))
            (register-dir! watch-service path)))))))

(defn- relevant-change? [^WatchKey key]
  (some (fn [event]
          (let [kind (.kind event)]
            (not= kind StandardWatchEventKinds/OVERFLOW)))
        (.pollEvents key)))

(defn- drain-events! [^WatchService ws first-key]
  (loop [changed? (boolean (relevant-change? first-key))]
    (let [next-key (.poll ws debounce-ms TimeUnit/MILLISECONDS)]
      (if next-key
        (recur (or changed? (boolean (relevant-change? next-key))))
        changed?))))

(defn- start-watcher! []
  (when-not @watcher
    (let [watch-service (.newWatchService (FileSystems/getDefault))
          executor      (Executors/newSingleThreadExecutor)]
      (doseq [dir watch-dirs]
        (register-dir-tree! watch-service dir))

      (.submit
       executor
       ^Runnable
       (fn []
         (println "Watching for changes in content/ and static/")
         (try
           (loop []
             (when-let [^WatchKey key (.take watch-service)]
               (let [changed? (drain-events! watch-service key)]
                 (when changed?
                   (try
                     (println "Change detected. Regenerating site...")
                     (generate!)
                     (println "Site regenerated.")
                     (catch Exception e
                       (println "Regeneration failed:" (.getMessage e)))))
                 (when (.reset key)
                   (recur)))))
           (catch ClosedWatchServiceException _
             (println "Watcher stopped."))
           (catch InterruptedException _
             (println "Watcher interrupted."))
           (catch Exception e
             (println "Watcher error:" (.getMessage e))))))

      (reset! watcher watch-service)
      (reset! watcher-executor executor))))

(defn- stop-watcher! []
  (when-let [^WatchService ws @watcher]
    (.close ws)
    (reset! watcher nil))
  (when-let [^ExecutorService ex @watcher-executor]
    (.shutdownNow ex)
    (.awaitTermination ex 1 TimeUnit/SECONDS)
    (reset! watcher-executor nil)))

(defn stop! []
  (stop-watcher!)
  (when @server
    (@server)
    (reset! server nil)
    (println "Server stopped.")))

(defn start!
  ([] (start! 8080))
  ([port]
   (generate!)
   (stop!)
   (reset! server (http/run-server #'handler {:port port}))
   (start-watcher!)
   (println (str "Serving at http://localhost:" port))
   (println "Hot reload enabled (manual browser refresh).")))

(comment
  (start!)      ;; starts on 8080
  (start! 4000)
  (generate!)   ;; manual regenerate
  (stop!))

