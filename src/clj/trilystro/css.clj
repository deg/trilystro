;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:errmsg {:color "red"}]
  [:body {:color "black"}]
)
