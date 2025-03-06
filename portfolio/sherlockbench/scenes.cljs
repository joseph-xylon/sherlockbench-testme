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

(defscene float-input-field
  (ui/render-input-field "float" 0))

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

(defscene attempt-page-complete
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 2"
    :arg-spec ["string" "boolean"]
    :state :completed}
   {:log [[:p "Hello"]
          [:p "do you"]
          [:p "like cats?"]]
    :next-verification {:inputs [4 7]
                        :output-type "boolean"}}))

(defscene attempt-page-abandoned
  (ui/render-attempt-page 
   "748d4792-b63b-40c5-bb51-cab946bd3d30"
   {:attempt-id "abc123"
    :problem-name "Problem 2"
    :arg-spec ["string" "boolean"]
    :state :abandoned}
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

(defscene results-page
  (let [mock-store {:run-id "748d4792-b63b-40c5-bb51-cab946bd3d30"
                    :run-type "practice"
                    :benchmark-version "1.0"
                    :run-state :submitted,
                    :final-score {:numerator 1, :denominator 3},
                    :attempts [{:attempt-id "f19e2768-c7e9-4547-8cad-21e70eb3b97b",
                                :arg-spec ["integer" "integer" "integer"],
                                :problem-name "Problem 1",
                                :state :completed,
                                :result :correct,
                                :function_name "add & subtract"}
                               {:attempt-id "73c9036d-3974-4ec8-abf9-d03b6794629a",
                                :arg-spec ["integer"],
                                :problem-name "Problem 2",
                                :state :completed,
                                :result :wrong,
                                :function_name "is prime"}
                               {:attempt-id "139d3722-6c7a-4c31-a60a-b681c03326d5",
                                :arg-spec ["integer"],
                                :problem-name "Problem 3",
                                :state :abandoned,
                                :result :abandoned,
                                :function_name "modulus 3 to fruit"}]}]
    (ui/render-results-page "748d4792-b63b-40c5-bb51-cab946bd3d30" mock-store)))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/styles.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))
