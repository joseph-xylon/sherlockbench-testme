(ns sherlockbench.scenes
  (:require [portfolio.replicant :refer-macros [defscene]]
            [portfolio.ui :as portfolio]
            [sherlockbench.ui :as ui]
            [replicant.dom :refer [set-dispatch!]]))

(def pass (constantly nil))

;; stub so it won't error
(set-dispatch! pass)

;; Input field components
(defscene text-input-field
  (ui/render-input-field "string" 0))

(defscene number-input-field
  (ui/render-input-field "integer" 0))

(defscene boolean-input-field
  (ui/render-input-field "boolean" 0))

(defscene input-form
  (ui/render-input-form "abc" "def" ["integer" "string" "boolean"]))

(defscene empty-log
  (ui/render-log-content []))

(defscene show-log
  (ui/render-log-content [[:p "Hello"]
                          [:p "do you"]
                          [:p "like cats?"]]))

;; Control buttons for different states
(defscene buttons-investigating
  (ui/control-buttons "748d4792-b63b-40c5-bb51-cab946bd3d30" :investigate))

(defscene buttons-verifying
  (ui/control-buttons "748d4792-b63b-40c5-bb51-cab946bd3d30" :verify))

;; Complete attempt page examples
(defscene attempt-page-investigating
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 1"
    :fn-args ["integer" "integer"]
    :state :investigate}
   '([:p "Hello"]
     [:p "do you"]
     [:p "like cats?"])))

(defscene attempt-page-verifying
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 2"
    :fn-args ["string" "boolean"]
    :state :verify}
   '([:p "Hello"]
     [:p "do you"]
     [:p "like cats?"])))

;; Index page example
(defscene index-page
  (let [mock-store (atom {:run-id "748d4792-b63b-40c5-bb51-cab946bd3d30"
                          :run-type "practice"
                          :benchmark-version "1.0"
                          :attempts [{:attempt-id "abc123"
                                      :problem-name "Problem 1"
                                      :fn-args ["integer" "integer"]
                                      :state :investigate}
                                     {:attempt-id "def456"
                                      :problem-name "Problem 2"
                                      :fn-args ["string" "boolean"]
                                      :state :verify}
                                     {:attempt-id "ghi789"
                                      :problem-name "Problem 3"
                                      :fn-args ["boolean" "boolean"]
                                      :state :completed}
                                     {:attempt-id "hdy623"
                                      :problem-name "Problem 4"
                                      :fn-args ["boolean" "boolean"]
                                      :state :abandoned}]})]
    (ui/render-index-page "748d4792-b63b-40c5-bb51-cab946bd3d30" mock-store)))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
