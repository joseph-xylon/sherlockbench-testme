(ns sherlockbench.core
  (:require [replicant.dom :as r]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as reitit-easy]
            [sherlockbench.logic :as logic]
            [sherlockbench.ui :as ui]))

(defn main []
  (let [store (atom nil)
        el (js/document.getElementById "app")]

    ;; Globally handle DOM events
    (r/set-dispatch!
     (fn [_ [action & args]]
       (case action
         :boop (apply swap! store logic/boop args))))

    ;; Define routes
    (let [routes [["/" {:name :home
                        :view (fn [] (home-page store el))}]
                  ["/about" {:name :about
                             :view (fn [] (about-page el))}]]]

      ;; Initialize Reitit router
      (reitit-easy/start!
       (reitit/router routes)
       (fn [match]
         (let [view-fn (get-in match [:data :view])]
           (when view-fn
             (view-fn))))
       {:use-fragment true}))))

;; I should be able to use this to redirect to new client-side page
;; (set! js/window.location.hash "#/more-info")
