(ns ring-raml.validator
  (:require [ring-raml.matcher :as m])
  (:import [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jsonschema.main JsonSchemaFactory]
           [com.github.fge.jsonschema.load.configuration LoadingConfiguration]
           [com.github.fge.jsonschema.load.uri URITransformer]))
(defn get-raml[])
(def json-schema-factory
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

(defn get-raml-def [req]
  (let [req_uri (:uri req)
        req_method (:request-method req)]
    (get-in (get-raml) [req_uri req_method :body :application/json :schema])))

(defn check-content [req resp]
  (let [raml_def_schema  (get-raml-def req)]
    (validate raml_def_schema  (:body resp))))

(defn validate-json [ data schema ]
  "Validate json against json-schema")

(defn validate-req [req raml]
  (let [raml_def (m/match req raml)]

    ))

(defn validate-resp[req resp raml])

(defn invalid-req? [req])
