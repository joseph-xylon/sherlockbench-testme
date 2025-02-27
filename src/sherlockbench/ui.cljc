(ns sherlockbench.ui
  (:require [reitit.frontend.easy :as reitit-easy]
            [clojure.string :as str]
            [cljs.pprint :refer [pprint]]))

;; This file contains pure functions which take some state and return hiccup
;; https://replicant.fun/hiccup/
;;
;; There are two special functions:
;; - the top-level function, which everything else is a child of. This is
;;   re-rendered when our atom changes
;; - optionally a data transformation function. this just adapts
;;   the "business domain data" to the right format for the UI

;; cute: "✓ Correct" "✗ Incorrect"

(defn render-attempt-link [{:keys [attempt-id
                                   fn-args
                                   problem-name
                                   completed] :as attempt} run-id]
  [:li {:key attempt-id}
   [:a {:href "#"
        :on {:click [[:action/prevent-default]
                     [:action/goto-page :attempt {:run-id run-id :attempt-id attempt-id}]]}}
    [:span problem-name]
    (when completed
      [:span {:style {:color "green"
                      :font-weight "bold"}}
       "✓ Completed"])]])

(defn render-attempts-list [run-id attempts]
  [:div
   [:h2 "Problems:"]
   [:ul {:style {:padding "0"}}
    (map #(render-attempt-link % run-id) attempts)]])

(defn index-content
  [run-id store]
  (let [attempts (:attempts @store)
        run-type (:run-type @store)]

    [:div
     [:h1 "SherlockBench Test"]
     (when (seq attempts)
       (render-attempts-list run-id attempts))

     [:div {:style {:margin-top "20px"}}
      [:p "Complete all problems to finish the test. You can work on problems in any order."]]
     ]))

(defn render-input-field [arg-type idx]
  (let [id (str "input-" idx)
        placeholder (case arg-type
                      "string" "Enter text..."
                      "integer" "Enter number..."
                      "boolean" "true or false"
                      "Enter value...")
        input-type (case arg-type
                     "integer" "number"
                     "text")]
    [:div.form-group
     [:label {:for id} (str "Input " (inc idx) " (" arg-type "):")]
     (if (= arg-type "boolean")
       [:select {:id id
                 :name id}
        [:option {:value "true"} "true"]
        [:option {:value "false"} "false"]]
       [:input {:type input-type
                :id id
                :name id
                :placeholder placeholder}])]))

(defn render-input-form [fn-args]
  [:form.attempt-form
   [:h2 "Test the Mystery Function"]
   (map-indexed 
    (fn [idx arg-type]
      ^{:key (str "input-field-" idx)}
      (render-input-field arg-type idx))
    fn-args)
   
   [:button.submit-btn {:type "submit"} "Submit"]])

(defn render-log-content [log]
  [:div.log-container
   log])

(defn control-buttons [run-id state]
  (list
   [:button.control {:on {:click [[:action/goto-page :index {:run-id run-id}]]}} "← Back to problem list"]
   (when (= state :investigate) [:button.control {} "I'm Ready"])
   [:button#abandon.control {} "Abandon"]))

(comment
  ;; example attempt map
  {:attempt-id "cbb0a1dc-5d1f-4721-942e-d0b6e5ae36df",
   :fn-args ["integer" "integer" "integer"]
   :problem-name "Problem 1" 
   :state :investigate ; :verify :completed :aborted
   }
  )

(defn render-attempt-page
  [run-id {:keys [attempt-id fn-args problem-name state] :as attempt} log]
  [:div.attempt-page
   [:h1 (str "Attempt: " problem-name)]
   [:p "Test the mystery function until you think you know what it does. Then
   click \"I'm Ready\" and the system will test you."]
   [:div.attempt-container
    ;; Input section
    [:div.attempt-input-section
     (render-input-form fn-args)]
    
    ;; Log section
    [:div.attempt-log-section
     [:h2 "Test Log"]
     (render-log-content log)]]
   
   [:div.attempt-navigation
    (control-buttons run-id state)]])
