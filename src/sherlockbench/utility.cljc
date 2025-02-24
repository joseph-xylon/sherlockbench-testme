(ns sherlockbench.utility
   (:require [clojure.string :as str]))

 (defn valid-uuid? [uuid-string]
   (try
     (let [uuid-regex
           #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"]
       (if (string? uuid-string)
         (boolean (re-matches uuid-regex (str/lower-case uuid-string)))
         false))
     (catch js/Error e
       false)))
