# YOLO11 car detection helper

This small helper runs a YOLO11 model (weights file `yolo11n.pt`) on a single image and filters detections for COCO class "car" (class id = 2).

Files added:
- `detect_car.py` — main detection script (CLI)
- `requirements.txt` — Python dependencies

Quick start (PowerShell):

1. Create a virtual environment and activate it:

```powershell
python -m venv .venv; .\.venv\Scripts\Activate.ps1
```

2. Install dependencies:

```powershell
pip install -r requirements.txt
```

3. Run detection (example):

```powershell
python detect_car.py --source C:\path\to\your\image.jpg --weights yolo11n.pt --conf 0.25
```

Outputs:
- Console output listing each detected car with confidence and bounding box.
- Annotated image saved as `annotated_<input_filename>.jpg` by default.

Notes:
- The script expects the weights file `yolo11n.pt` to be present in the workspace or pass its path via `--weights`.
- The script filters COCO class id 2 (car). If your model uses a different class mapping, update the class id in `detect_car.py`.

## Export to TensorFlow Lite (for Android)

Use the provided exporter to convert your `.pt` to `.tflite` with NMS included:

```cmd
cd %~dp0
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
python export_to_tflite.py --weights yolo11n.pt --imgsz 640 --out yolo11n.tflite
```

Then copy the resulting `yolo11n.tflite` to:

```
../app/src/main/assets/models/car_detector.tflite
```

Rebuild the Android app and run. The app will draw boxes when the TFLite model is present.
