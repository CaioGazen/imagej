#!/bin/bash

source ../../venv/bin/activate

DATASET="../../datasets/hotwheels"
cd yolov5 || exit
python train.py --img 640 --batch 10 --epochs 500 --data "$DATASET"/data.yaml --weights yolov5s.pt --cache

#python train.py --img 640 --batch 20 --epochs 500 --data "$DATASET"/data.yaml --weights runs/train/exp15/weights/last.pt --cache
