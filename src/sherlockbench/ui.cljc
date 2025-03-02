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
                                   problem-name
                                   arg-spec
                                   state] :as attempt} run-id]
  [:li {:key attempt-id}
   [:a (when (#{:investigate :verify} state)
         {:href "#"
          :on {:click [[:action/prevent-default]
                       [:action/goto-page :attempt {:run-id run-id :attempt-id attempt-id}]]}})
    [:span problem-name]
    (case state
      :completed
      [:span {:style {:color "green"
                      :font-weight "bold"}}
       " ✓ Completed"]

      :abandoned
      [:span {:style {:color "darkred"
                      :font-weight "bold"}}
       " ⊗ Abandoned"]
      nil)]])

(defn render-attempts-list [run-id attempts]
  [:div
   [:h2 "Problems:"]
   [:ul {:style {:padding "0"}}
    (map #(render-attempt-link % run-id) attempts)]])

(defn render-index-page
  [run-id run-data]
  (let [attempts (:attempts run-data)
        run-type (:run-type run-data)]

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

(defn investigation-input-form [run-id attempt-id arg-spec]
  [:form.attempt-form
   [:h2 "Test the Mystery Function"]
   (map-indexed
    #(render-input-field %2 %1)
    arg-spec)
   
   [:button.submit-btn {:on {:click [[:action/prevent-default]
                                     [:action/test-mystery-function run-id attempt-id]]}} "Submit"]])

(defn verification-input-form [run-id attempt-id fn-args]
  [:form.attempt-form
   [:h2 "What will the output be with these inputs?"]
   [:p ]
   
   [:button.submit-btn {:on {:click [[:action/prevent-default]
                                     [:action/predict-fn-output run-id attempt-id]]}} "Submit"]])

(defn render-log-content [log]
  [:div.log-container
   (seq log)])

(defn control-buttons [run-id attempt-id state]
  (list
   [:button.control {:on {:click [[:action/goto-page :index {:run-id run-id}]]}} "← Back to problem list"]
   (when (= state :investigate)
     [:button.control {:on {:click [[:action/get-verification run-id attempt-id]]}} "I'm Ready"])
   [:button#abandon.control {} "Abandon"]))

(comment
  ;; example attempt map
  {:attempt-id "cbb0a1dc-5d1f-4721-942e-d0b6e5ae36df",
   :arg-spec ["integer" "integer" "integer"]
   :problem-name "Problem 1" 
   :state :investigate ; :verify :completed :abandoned
   }

  ;; state values: :investigate :verify :completed :abandoned

  ;; example attempt-data
  {:log []
   :next-verification {:inputs [2 5]
                       :output-type "integer"}}  ; "string" "boolean" "float"
  )

(defn render-attempt-page
  [run-id
   {:keys [attempt-id arg-spec problem-name state] :as attempt}
   {:keys [log] :as attempt-data}]

  [:div.attempt-page
   [:h1 (str "Attempt: " problem-name)]
   (if (= state :investigate)
       [:p "Test the mystery function until you think you know what it does. Then
   click \"I'm Ready\" and the system will test you."]
       [:p "Prove you have figured out what the function does."])
   [:div.attempt-container
    ;; Input section
    [:div.attempt-input-section
     (if (= state :investigate)
       (investigation-input-form run-id attempt-id arg-spec)
       (verification-input-form run-id attempt-id arg-spec))]
    
    ;; Log section
    [:div.attempt-log-section
     [:h2 "Test Log"]
     (render-log-content log)]]
   
   [:div.attempt-navigation
    (control-buttons run-id attempt-id state)]])
