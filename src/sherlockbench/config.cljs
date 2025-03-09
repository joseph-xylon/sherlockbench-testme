(ns sherlockbench.config
  (:require-macros [cljs.core :refer [goog-define]]))

;; The api-url is defined by closure compiler defines based on build type
;; See shadow-cljs.edn for the values
(goog-define api-url "http://localhost:3000")

(defn api-endpoint [path]
  (str api-url path))