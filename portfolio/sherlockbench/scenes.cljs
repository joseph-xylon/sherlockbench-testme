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
  (ui/investigation-input-form "abc" "def" ["integer" "string" "boolean"]))

(defscene empty-log
  (ui/render-log-content []))

(defscene show-log
  (ui/render-log-content [[:p "Hello"]
                          [:p "do you"]
                          [:p "like cats?"]]))

;; Control buttons for different states
(defscene buttons-investigating
  (ui/control-buttons "748d4792-b63b-40c5-bb51-cab946bd3d30" "748d4792-b63b-40c5-bb51-cab946bd3d30" :investigate))

(defscene buttons-verifying
  (ui/control-buttons "748d4792-b63b-40c5-bb51-cab946bd3d30" "748d4792-b63b-40c5-bb51-cab946bd3d30" :verify))

;; Complete attempt page examples
(defscene attempt-page-investigating
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 1"
    :arg-spec ["integer" "integer"]
    :state :investigate}
   {:log [[:p "Hello"]
          [:p "do you"]
          [:p "like cats?"]]}))

(defscene attempt-page-verifying-number
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 2"
    :arg-spec ["string" "boolean"]
    :state :verify}
   {:log [[:p "Hello"]
          [:p "do you"]
          [:p "like cats?"]]
    :next-verification {:inputs [4 7]
                        :output-type "integer"}}))

(defscene attempt-page-verifying-boolean
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 2"
    :arg-spec ["string" "boolean"]
    :state :verify}
   {:log [[:p "Hello"]
          [:p "do you"]
          [:p "like cats?"]]
    :next-verification {:inputs [4 7]
                        :output-type "boolean"}}))

;; Index page example
(defscene index-page
  (let [mock-store {:run-id "748d4792-b63b-40c5-bb51-cab946bd3d30"
                    :run-type "practice"
                    :benchmark-version "1.0"
                    :attempts [{:attempt-id "abc123"
                                :problem-name "Problem 1"
                                :arg-spec ["integer" "integer"]
                                :state :investigate}
                               {:attempt-id "def456"
                                :problem-name "Problem 2"
                                :arg-spec ["string" "boolean"]
                                :state :verify}
                               {:attempt-id "ghi789"
                                :problem-name "Problem 3"
                                :arg-spec ["boolean" "boolean"]
                                :state :completed}
                               {:attempt-id "hdy623"
                                :problem-name "Problem 4"
                                :arg-spec ["boolean" "boolean"]
                                :state :abandoned}]}]
    (ui/render-index-page "748d4792-b63b-40c5-bb51-cab946bd3d30" mock-store)))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
