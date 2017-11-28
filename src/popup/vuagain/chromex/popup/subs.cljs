(ns vuagain.chromex.popup.subs
  (:require
   [re-frame.core :as re-frame]
   [iron.re-utils :as re-utils :refer [sub2 <sub >evt]]))

(sub2 :name [:name])
(sub2 :user [:user])
(sub2 :background-port [:background-port])

