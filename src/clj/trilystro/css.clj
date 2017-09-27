;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:body
   {:background-color "PapayaWhip"
    :color "black"}]
  [:.hidden
   {:display "none"}]
  [:.errmsg
   {:color "red"}]
)
