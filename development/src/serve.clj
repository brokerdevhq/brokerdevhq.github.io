(ns serve
  (:require [ca.brokerdev.site-generator.interface :as generator]
            [org.httpkit.server :as http]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.file Files Paths]))

(def site-config
  {:site-title "BrokerDev"
   :css ["/css/style.css"]})

(defonce server (atom nil))

(defn- mime-type [path]
  (cond
    (str/ends-with? path ".html") "text/html; charset=utf-8"
    (str/ends-with? path ".css")  "text/css"
    (str/ends-with? path ".js")   "application/javascript"
    (str/ends-with? path ".png")  "image/png"
    (str/ends-with? path ".jpg")  "image/jpeg"
    (str/ends-with? path ".ico")  "image/x-icon"
    :else                         "application/octet-stream"))

(defn- handler [req]
  (let [uri      (if (= "/" (:uri req)) "/index.html" (:uri req))
        path     (str "public" uri)
        file     (io/file path)]
    (if (.exists file)
      {:status  200
       :headers {"Content-Type" (mime-type path)}
       :body    (Files/readAllBytes (.toPath file))}
      {:status  404
       :headers {"Content-Type" "text/plain"}
       :body    (str "Not found: " uri)})))

(defn generate! []
  (generator/generate-site
   {:content-dir "content"
    :output-dir  "public"
    :static-dir  "static"
    :site-config site-config}))

(defn start!
  ([] (start! 8080))
  ([port]
   (generate!)
   (when @server (@server))
   (reset! server (http/run-server #'handler {:port port}))
   (println (str "Serving at http://localhost:" port))))

(defn stop! []
  (when @server
    (@server)
    (reset! server nil)
    (println "Server stopped.")))

(comment
  (start!)        ;; generate + serve on port 3000
  (generate!)     ;; regenerate without restarting server
  (stop!)
  (start! 4000)   ;; use a different port
  )
