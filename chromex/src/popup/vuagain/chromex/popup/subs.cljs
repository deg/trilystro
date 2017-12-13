(ns vuagain.chromex.popup.subs
  (:require
   [re-frame.core :as re-frame]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]
   [trilib.firebase :as fb]))

(re-frame/reg-sub
 :all-db
 (fn [db _]
   db))

(sub2 :name [:name])
(sub2 :background-port [:background-port])
(sub2 :current-tab [:current-tab])

(re-frame/reg-sub
 :url
 :<- [:current-tab]
 (fn [tab _] (:url tab)))

(re-frame/reg-sub
 :title
 :<- [:current-tab]
 (fn [tab _] (:title tab)))

(re-frame/reg-sub
 :page-param
 (fn [db [_ param]]
   (get-in db [:page-params param])))
