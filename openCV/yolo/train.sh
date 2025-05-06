#!/bin/bash

source ../../venv/bin/activate

DATASET="../../datasets/hotwheels"
cd yolov5 || exit
python train.py --img 640 --batch 6 --epochs 50 --data "$DATASET"/data.yaml --weights yolov5l.pt --cache
