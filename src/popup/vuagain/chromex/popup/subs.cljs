(ns vuagain.chromex.popup.subs
  (:require
   [re-frame.core :as re-frame]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]))

(sub2 :name [:name])
(sub2 :user [:user])
(sub2 :background-port [:background-port])
(sub2 :current-tab [:current-tab])

(re-frame/reg-sub
 :user-name
 :<- [:user]
 (fn [user _]
   (or (:display-name user) (:email user))))

(re-frame/reg-sub
 :url
 :<- [:current-tab]
 (fn [tab _] (:url tab)))

(re-frame/reg-sub
 :title
 :<- [:current-tab]
 (fn [tab _] (:title tab)))
