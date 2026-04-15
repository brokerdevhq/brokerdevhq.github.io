(ns ca.brokerdev.template.interface
  (:require [hiccup.page :refer [html5]]))

;; Context helpers

(defn- resolve-url [context url]
  (if-let [resolve-fn (:resolve-url context)]
    (resolve-fn url)
    url))

(defn- get-css-paths [context]
  (let [css-urls (get-in context [:site-config :css] ["/css/style.css"])]
    (map #(resolve-url context %) css-urls)))

(defn- get-site-title [context]
  (get-in context [:site-config :site-title] "BrokerDev"))

;; Page head

(defn- page-head [context]
  (let [{:keys [title description]} context
        site-title (get-site-title context)
        css-paths  (get-css-paths context)
        full-title (when title
                     (if (= title site-title) title (str title " | " site-title)))]
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     (when full-title [:title full-title])
     (when description [:meta {:name "description" :content description}])
     (for [css-path css-paths]
       [:link {:rel "stylesheet" :href css-path}])
     [:link {:rel "stylesheet" :href (resolve-url context "/css/highlight.min.css")}]]))

;; Header

(defn- page-header [context]
  [:header.site-header
   [:div.container.site-header-inner
    [:a.logo {:href (resolve-url context "/index.html")} "BrokerDev"]

    [:button.nav-toggle
     {:type "button"
      :aria-label "Toggle navigation"
      :aria-expanded "false"
      :onclick "document.body.classList.toggle('nav-open'); this.setAttribute('aria-expanded', document.body.classList.contains('nav-open')); "}
     [:span]
     [:span]
     [:span]]

    [:nav.site-nav
     [:a {:href (resolve-url context "/index.html")} "Home"]
    ;;  [:a {:href (resolve-url context "/services.html")} "Services"]
    ;;  [:a {:href (resolve-url context "/about.html")} "About"]
    ;;  [:a {:href (resolve-url context "/posts.html")} "Insights"]
     [:a.nav-cta {:href "mailto:info@brokerdev.ca"} "Contact"]]]])


;; Footer

(defn- page-footer [context]
  [:footer.site-footer
   [:div.container
    [:div.footer-brand
     [:a.logo {:href (resolve-url context "/index.html")} "BrokerDev"]
     [:p "Modern software for independent insurance brokers."]]
    ;; [:div.footer-section
    ;;  [:h4 "Pages"]
    ;;  [:a {:href (resolve-url context "/services.html")} "Services"]
    ;;  [:a {:href (resolve-url context "/about.html")} "About"]
    ;;  [:a {:href (resolve-url context "/posts.html")} "Insights"]]
    [:div.footer-section
     [:h4 "Contact"]
     [:a {:href "mailto:info@brokerdev.ca"} "info@brokerdev.ca"]]
    [:div.footer-bottom
     [:p "© 2025 BrokerDev"]]]])

;; Post bio footer

(defn- post-footer [context]
  [:section.post-footer
   [:hr]
   [:div.author-bio
    [:h3 "About BrokerDev"]
    [:p "BrokerDev builds modern software for independent insurance brokers — from Applied Epic integrations to workflow automation and legacy system modernization."]
    [:p [:a {:href "mailto:info@brokerdev.ca"} "Get in touch"] " to discuss what we can build for your brokerage."]]])

;; Posts list (home + insights page)

(defn- posts-list [context posts]
  (when (seq posts)
    [:section.posts
     [:h2 "Insights"]
     [:ul.post-list
      (for [post posts]
        [:li
         [:article
          [:h3 [:a {:href (resolve-url context (:url post))} (:title post)]]
          (when (:date post)
            [:time {:datetime (:date post)} (:date post)])
          (when (:description post)
            [:p (:description post)])]])]]))

;; Rendering functions

(defn render-page
  "Render a standard page. Accepts optional :content-class to wrap content-hiccup."
  [context]
  (let [{:keys [content-hiccup posts content-class show-footer?]} context
        show-footer? (if (nil? show-footer?) true show-footer?)
        wrapped-content (if content-class
                          [:div {:class content-class} content-hiccup]
                          content-hiccup)]
    (html5 {:lang "en"}
      (page-head context)
      [:body
       (page-header context)
       [:main
        [:div.container
         wrapped-content
         (when posts (posts-list context posts))]]
       (when show-footer? (page-footer context))
       [:script {:src (resolve-url context "/js/highlight.min.js")}]
       [:script "hljs.highlightAll();"]])))

(defn render-home
  "Render the home page with a hero section and posts grid below."
  [context]
  (let [{:keys [content-hiccup posts]} context]
    (html5 {:lang "en"}
      (page-head context)
      [:body.home
       (page-header context)
       [:section.hero
        [:div.container
         [:div.hero-content
          content-hiccup
          [:div.hero-cta
          ;;  [:a.btn.btn-primary {:href (resolve-url context "/services.html")} "Our Services"]
           [:a.btn.btn-secondary {:href "mailto:info@brokerdev.ca"} "Get in Touch"]]
          ]
         [:div.hero-visual
          [:img.hero-image
           {:src (resolve-url context "/img/5cb91d8f-242c-462b-8314-6d4b336a86bd.png")
            :alt "Hero image"
            :style "max-width:100%; height:auto;"}]]]]
       [:main
        [:div.container
         (when posts (posts-list context posts))]]
       (page-footer context)
       [:script {:src (resolve-url context "/js/highlight.min.js")}]
       [:script "hljs.highlightAll();"]])))

(defn render-services
  "Render the services page with card-styled service sections."
  [context]
  (render-page (assoc context :content-class "services-content")))

(defn render-post
  "Render a blog post."
  [context]
  (let [{:keys [title date content-hiccup]} context
        article-hiccup [:div.post-content
                        [:article
                         [:h1 title]
                         (when date [:time {:datetime date} date])
                         content-hiccup
                         (post-footer context)]]]
    (render-page (assoc context :content-hiccup article-hiccup))))
