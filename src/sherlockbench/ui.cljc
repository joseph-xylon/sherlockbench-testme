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

(defn render-attempt-link [{:keys [attempt-id
                                   fn-args
                                   problem-name
                                   completed] :as attempt} run-id]
  [:li {:key attempt-id}
   [:a {:href (reitit-easy/href :attempt {:run-id run-id :attempt-id attempt-id})}
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

(defn render-attempt-page
  [run-id {:keys [attempt-id fn-args problem-name] :as attempt} log]
  [:div
   [:h1 (str "Attempt " problem-name)]
   ]
  )
