#!/bin/bash

#clone YOLOv5 and
git clone https://github.com/ultralytics/yolov5
cd yolov5 || exit
pip install -qr requirements.txt -v
