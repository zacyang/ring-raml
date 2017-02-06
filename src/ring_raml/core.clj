(ns ring-raml.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [raml-clj-parser.core :as raml]
            [cheshire.core :refer [generate-string]])
  (:import [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jsonschema.main JsonSchemaFactory]
           [com.github.fge.jsonschema.load.configuration LoadingConfiguration]
           [com.github.fge.jsonschema.load.uri URITransformer]
           ))

;;;schema validation
(def
  json-schema-factory
  (let [transformer (-> (URITransformer/newBuilder)
                        (.setNamespace "resource:/schema/")
                        .freeze)
        loading-config (-> (LoadingConfiguration/newBuilder)
                           (.setURITransformer transformer)
                           .freeze)
        factory (-> (JsonSchemaFactory/newBuilder)
                    (.setLoadingConfiguration loading-config)
                    .freeze)]
    factory))

(defn- parse-to-node
  [ data]
  (.readTree (.reader (ObjectMapper.)) data))

(defn validate
  [schema data]
  (let [parsed-schema (parse-to-node schema)
        schema (-> json-schema-factory (.getJsonSchema parsed-schema))
        parsed-data (parse-to-node data)
        report (.validate schema parsed-data)]
    {:success (.isSuccess report)
     :message (str report)}))

(defn get-raml[]
  (raml/read-raml "resources/sample.raml"))

(def ^:dynamic *request-id* nil)

(defn get-raml-def [req]
  (let [req_uri (:uri req)
        req_method (:request-method req)]
    (get-in (get-raml) [req_uri req_method :body :application/json :schema])))


(defn check-content [req resp]
  (let [raml_def_schema  (get-raml-def req)]
    (validate raml_def_schema (generate-string (:body resp)))))

(defn not-define-in-raml [req]
  (str  "request for " (:uri req) "method " (:request-method req) "is not defined in raml"))

(defn req-def-in-raml? [req]
  (not (nil? (get-in (get-raml) [(:uri req)]))))

(defn validate-with-raml [req resp]
  (if (req-def-in-raml? req)
    (check-content req resp)
    (not-define-in-raml req)))

(defn simple-raml-middleware [app]
  (fn [req]
    (binding [*request-id* (rand-int 0xffff)]
      ;;(clojure.pprint/pprint req)
      ;;(cond (= "/" (:uri req)) (resp-from-raml req) )
      (let [resp_from_app (app req)
            validate_result (validate-with-raml req resp_from_app)]
        (prn "from app\n"  resp_from_app )
        (prn "validation result" validate_result)
        (prn "resp "  (str "raml schema validation error" (:message validate_result)))
        (if (:success validate_result)
          (assoc resp_from_app :body (generate-string (:body resp_from_app)))
          {:status 503 :body (str "raml schema validation error" (:message validate_result))}
          )))))

(def correct_resp {
                   :familyid "Dev",
                   :relationship [
                                    {
                                     :type "affects",
                                     :id 44
                                     }
                                    ]
                   })

(def incorrect_resp {
                     :some-key "bla"
                   })

(defroutes app
  (GET "/relationships" {} {:body
                            incorrect_resp})
  (route/not-found "Page not found"))

(def middleware-app
  (-> app
      simple-raml-middleware))

(defn start-server [] (future (jetty/run-jetty (var middleware-app) {:port 8081})))
