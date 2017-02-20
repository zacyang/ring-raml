(ns ring-raml.validator
  (:require [ring-raml.matcher :as m]
            [clojure.pprint :as pp]
            [ring.middleware.params :as params])

  (:import [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jsonschema.main JsonSchemaFactory]
           [com.github.fge.jsonschema.load.configuration LoadingConfiguration]
           [com.github.fge.jsonschema.load.uri URITransformer]
           [org.eclipse.jetty.util UrlEncoded MultiMap]))
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
  "Return [raml_defined matched_path_key] "
  (if-let [raml_def (get  raml (first path))]
    [raml_def (first path)]
    (get-uri-parameter-sources raml)))

(defn get-raml-def
  ([req_uri_path raml]
   (get-raml-def [] req_uri_path raml))

  ([found_path req_uri_path raml]
   (if (not-any? empty? [req_uri_path raml])
     (let [[next_level_raml raml_path] (next-level-raml raml req_uri_path)]
       (get-raml-def (conj found_path raml_path)
                     ;;TODO: depends the match type e.g /user /abc could match to /user and subordanate key /abc, it could also match /user/abc if key defined so, even it is not recommanded by the RAML community
                     (rest req_uri_path)
                     next_level_raml))
     {::path found_path
      ::raml_def raml})))

(defn match-req [req raml]
  "match request against raml def primary using uri"
  (let [matched_result (get-raml-def (get-req-raml-path req) raml)]
    (if-let [matched_raml (::raml_def matched_result)]
      matched_result
      (request-not-defined req raml))))


(defn validate-req-query-params [ match_result req raml]
  (let [raml_def_query_params (get-in match_result [::raml_def :queryParameters])
        req_query_params (:query-string req)
        parameter_map (#(apply hash-map (clojure.string/split % #"="))  (java.net.URLDecoder/decode req_query_params))]
    (when (every? #(= (get raml_def_query_params %)
                     (get parameter_map %)) (keys parameter_map)))
    req))

(defn validate-req [req raml]
  (let [match_result (match-req req raml)]
    (if-not (contains? match_result ::error)
      (validate-req-query-params match_result req raml)
      (request-not-defined  req raml))))

(defn validate-resp[req resp raml])

(defn invalid-req? [req])
