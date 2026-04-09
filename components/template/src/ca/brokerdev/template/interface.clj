(ns ca.brokerdev.template.interface
  (:require [hiccup.page :refer [html5]]))

;; Context-aware URL resolution

(defn- resolve-url
  "Resolve a URL using the context's resolve-url function."
  [context url]
  (if-let [resolve-fn (:resolve-url context)]
    (resolve-fn url)
    url))

(defn- get-css-paths
  "Get CSS paths from site config and resolve them."
  [context]
  (let [css-urls (get-in context [:site-config :css] ["/css/style.css"])]
    (map #(resolve-url context %) css-urls)))

(defn- get-site-title
  "Get site title from config."
  [context]
  (get-in context [:site-config :site-title] "BrokerDev"))

;; Hiccup building functions

(defn- page-head
  "Build the <head> section as hiccup."
  [context]
  (let [{:keys [title description]} context
        css-paths (get-css-paths context)]
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     (when title [:title (str title " | BrokerDev")])
     (when description [:meta {:name "description" :content description}])
     (for [css-path css-paths]
       [:link {:rel "stylesheet" :href css-path}])
     [:link {:rel "stylesheet" :href (resolve-url context "/css/highlight.min.css")}]]))

(defn- page-header
  "Build the header section as hiccup."
  [context]
  (let [site-title (get-site-title context)]
    [:header
     [:h1 [:a {:href (resolve-url context "/index.html")} site-title]]
     [:nav
      [:a {:href (resolve-url context "/index.html")} "Home"]
      [:a {:href (resolve-url context "/services.html")} "Services"]
      [:a {:href (resolve-url context "/about.html")} "About"]
      [:a {:href (resolve-url context "/posts.html")} "Insights"]]]))

(defn- page-footer
  "Build the footer section as hiccup."
  [context]
  [:footer
   [:p "© 2025 BrokerDev — Modern software for independent insurance brokers."]])

(defn- post-footer
  "Build the post-specific footer."
  [context]
  [:section.post-footer
   [:hr]
   [:div.author-bio
    [:h3 "About BrokerDev"]
    [:p "BrokerDev builds modern software for independent insurance brokers — from Applied Epic integrations to workflow automation and legacy system modernization."]
    [:p [:a {:href "mailto:hello@brokerdev.ca"} "Get in touch"] " to discuss what we can build for your brokerage."]]])

(defn- posts-list
  "Build a list of blog posts as hiccup."
  [context posts]
  (when (seq posts)
    [:section.posts
     [:h2 "Insights"]
     [:ul.post-list
      (for [post (sort-by :date posts)]
        [:li
         [:article
          [:h3 [:a {:href (resolve-url context (:url post))} (:title post)]]
          (when (:date post)
            [:time {:datetime (:date post)} (:date post)])
          (when (:description post)
            [:p (:description post)])]])]]))

;; Rendering functions

(defn render-page
  "Render a page with hiccup content."
  [context]
  (let [{:keys [content-hiccup posts show-footer?]} context
        show-footer? (if (nil? show-footer?) true show-footer?)]
    (html5 {:lang "en"}
      (page-head context)
      [:body
       [:div
        (page-header context)
        [:main
         content-hiccup
         (when posts
           (posts-list context posts))]
        (when show-footer?
          (page-footer context))]
       [:script {:src (resolve-url context "/js/highlight.min.js")}]
       [:script "hljs.highlightAll();"]])))

(defn render-post
  "Render a blog post with hiccup content."
  [context]
  (let [{:keys [title date content-hiccup]} context
        article-hiccup [:article
                        [:h1 title]
                        (when date [:time {:datetime date} date])
                        content-hiccup
                        (post-footer context)]]
    (render-page (assoc context :content-hiccup article-hiccup))))
