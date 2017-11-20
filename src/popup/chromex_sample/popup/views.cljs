(ns chromex-sample.popup.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn popup []
  (fn []
    [:h3 {:style {:min-width "30em"}}
     (str "Welcome to " @(re-frame/subscribe [:name]))]))
