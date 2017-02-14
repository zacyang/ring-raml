(ns ring-raml.validator
  (:require [ring-raml.matcher :as m]
            [clojure.pprint :as pp])

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
        schema (.getJsonSchema json-schema-factory  parsed-schema)
        parsed-data (parse-to-node data)
        report (.validate schema parsed-data)]
    {:success (.isSuccess report)
     :message (str report)}))

;; (defn get-raml-def [req]
;;   (let [req_uri (:uri req)
;;         req_method (:request-method req)]
;;     (get-in (get-raml) [req_uri req_method :body :application/json :schema])))

;; (defn check-content [req resp]
;;   (let [raml_def_schema  (get-raml-def req)]
;;     (validate raml_def_schema  (:body resp))))

(defn validate-json [ data schema ]
  "Validate json against json-schema")

(defn request-not-defined [req raml]
  {::error (str "Requested resource is not defined in raml spec " req)})

(defn- get-uri-path [req]
  (vec (map (fn [s] (str "/" s))
            (remove empty? (clojure.string/split (:uri req) #"/")))))

(defn- get-req-raml-path [req]
  (conj (get-uri-path req) (:request-method req)))

(defn get-uri-parameter-sources [raml]
  (let [uri_parameter_path (first  (filter #(when (string? %)
                                             (re-find #"\{.+\}" %)) (keys raml)))]
    [(get raml uri_parameter_path) uri_parameter_path]))

(defn next-level-raml [raml path]
  (if-let [raml_def (get  raml (first path))]
    [raml_def (first path)]
    (get-uri-parameter-sources raml)))

(defn get-raml-def [found_path req_uri_path raml]
  (if (not-any? empty? [req_uri_path raml])
    (let [[next_level_raml raml_path] (next-level-raml raml req_uri_path)]
      (get-raml-def (conj found_path raml_path)
                    (rest req_uri_path)
                    next_level_raml))
    {::path found_path
     ::raml_def raml}))

(defn match-req [req raml]
  "match request against raml def primary using uri"
  (let [matched_result (get-raml-def [] (get-req-raml-path req) raml)]
    (if-let [matched_raml (::raml_def matched_result)]
      matched_result
      (request-not-defined req raml))))

(defn validate-req-query-params [req raml]
  req)

(defn validate-req [req raml]
  (if-let [raml_def (match-req req raml)]
    (validate-req-query-params req raml)
    (request-not-defined req raml)))

(defn validate-resp[req resp raml])

(defn invalid-req? [req])
