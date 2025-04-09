(ns sherlockbench.config
  (:require-macros [cljs.core :refer [goog-define]]))

;; These values are replaced by the closure compiler. See shadow-cljs.edn
(goog-define api-url "http://localhost:3000")
(goog-define list-subsets true)

(defn api-endpoint [path]
  (str api-url path))
