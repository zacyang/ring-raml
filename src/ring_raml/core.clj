(ns ring-raml.core
  (:require [raml-clj-parser.core :as raml]
            [ring-raml.validator :as v]
            [cheshire.core :refer [generate-string]]))

(defn get-raml[]
  (raml/read-raml "resources/sample.raml"))

(defn invalid-raml-req-resp [req])

(defn raml-middleware [app & [{:as options}]]
  (fn [req]
    (let [req (v/validate-req req (get-raml))]
      (if (v/invalid-req? req)
        (invalid-raml-req-resp req))
      (let [app_resp (app req)]
        (v/validate-resp req app_resp (get-raml))))))


;;(defn start-server [] (future (jetty/run-jetty (var middleware-app) {:port 8081})))
