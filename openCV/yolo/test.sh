#!/bin/bash

source ../../venv/bin/activate

DATASET="../../datasets/hotwheels"
cd yolov5 || exit

python detect.py --weights runs/train/exp15/weights/best.pt --conf 0 --source "$DATASET"/images/val
