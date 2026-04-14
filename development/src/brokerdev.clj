(ns brokerdev
  (:require [ca.brokerdev.site-generator.interface :as generator]
            [ca.brokerdev.markdown-parser.interface :as parser]
            [ca.brokerdev.html-renderer.interface :as renderer]
            ;;[ca.brokerdev.template.interface :as template]
            [ca.brokerdev.file-utils.interface :as files]))

(comment

  ;; Generate the full site
  (generator/generate-site
   {:content-dir "content"
    :output-dir "public"
    :static-dir "static"
    :site-config {:site-title "BrokerDev"
                  :css ["/css/style.css"]}})

  ;; Quick generate with defaults
  (generator/generate-site {})

  ;; Test markdown parsing
  (parser/parse "{:title \"Test\"}\n# Hello")

  ;; Test markdown to hiccup conversion
  (renderer/markdown->hiccup "# Hello\n\nThis is a **test**")

  ;; Get all pages
  (generator/get-pages {:content-dir "content"})

  ;; List content files
  (files/list-files "content" {:extension ".md"})

  )
