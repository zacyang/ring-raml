(ns ring-raml.matcher)

(defn not-define-in-raml [req]
  (str  "request for " (:uri req) "method " (:request-method req) "is not defined in raml"))

(defn req-def-in-raml? [req]
;  (not (nil? (get-in (get-raml) [(:uri req)])))
)

(defn match [req raml])
