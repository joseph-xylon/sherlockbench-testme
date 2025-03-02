(ns sherlockbench.forms
  (:require [clojure.string :as str]))

(defn collect-input-form-values
  "Collects all input values from form elements with IDs matching input-{n}"
  []
  (loop [idx 0
         values []]
    (let [input-el (js/document.getElementById (str "input-" idx))]
      (if input-el
        (let [value (.-value input-el)
              form-type (-> input-el .-parentNode .-firstChild .-textContent
                            (str/split #"\(|\)") second)]
          (recur (inc idx)
                 (conj values 
                       (case form-type
                         "integer" (js/parseInt value)
                         "boolean" (= value "true")
                         value))))
        values))))

(defn collect-verification-form-value
  "Gets the value from a form element with ID expected-out"
  []
  (when-let [input-el (js/document.getElementById "expected-out")]
    (.-value input-el)))

(defn clear-verification-form
  "Clears the value from a form element with ID expected-out"
  []
  (when-let [input-el (js/document.getElementById "expected-out")]
    (set! (.-value input-el) "")))

(defn clear-input-form
  "Clears all input values from form elements with IDs matching input-{n}"
  [values-count]
  (doseq [idx (range values-count)]
    (when-let [input-el (js/document.getElementById (str "input-" idx))]
      (if (= (.-tagName input-el) "SELECT")
        (set! (.-selectedIndex input-el) 0)
        (set! (.-value input-el) "")))))