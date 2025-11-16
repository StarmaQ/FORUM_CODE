import argparse
import os
from pathlib import Path

import numpy as np
from PIL import Image

try:
    from ultralytics import YOLO
except Exception as e:
    raise SystemExit(
        "ultralytics is required. Install with: pip install -r requirements.txt\nOriginal error: %s" % e
    )


def parse_args():
    p = argparse.ArgumentParser(description="Run YOLO11 on an image and list/save car detections (COCO class 'car' id=2)")
    p.add_argument("--source", "-s", required=True, help="Path to image file")
    p.add_argument("--weights", "-w", default="yolo11n.pt", help="Path to weights file (default: yolo11n.pt)")
    p.add_argument("--conf", type=float, default=0.25, help="Confidence threshold (default: 0.25)")
    p.add_argument("--imgsz", type=int, default=640, help="Inference image size (default: 640)")
    p.add_argument("--output", "-o", default=None, help="Output annotated image path (default: annotated_<input>)")
    return p.parse_args()


def main():
    args = parse_args()
    source = args.source
    weights = args.weights
    conf = args.conf
    imgsz = args.imgsz

    if not os.path.isfile(source):
        raise SystemExit(f"Source image not found: {source}")
    if not os.path.isfile(weights):
        raise SystemExit(f"Weights file not found: {weights} (expected in workspace).")

    print(f"Loading model from {weights} ...")
    model = YOLO(weights)

    print(f"Running inference on {source} (imgsz={imgsz}, conf={conf}) ...")
    results = model.predict(source=source, imgsz=imgsz, conf=conf, verbose=False)

    if len(results) == 0:
        print("No results returned by model.")
        return

    res = results[0]

    # model.names gives class id -> name mapping (COCO 80 expected)
    names = getattr(model, "names", None) or getattr(model.model, "names", None) or {}

    cars = []
    if hasattr(res, "boxes") and len(res.boxes) > 0:
        # res.boxes.cls, res.boxes.conf, res.boxes.xyxy
        try:
            cls_arr = res.boxes.cls.cpu().numpy().astype(int)
            conf_arr = res.boxes.conf.cpu().numpy()
            xyxy = res.boxes.xyxy.cpu().numpy()
        except Exception:
            # If tensors are already numpy
            cls_arr = np.array(res.boxes.cls).astype(int)
            conf_arr = np.array(res.boxes.conf)
            xyxy = np.array(res.boxes.xyxy)

        for i, c in enumerate(cls_arr):
            if c == 2:  # COCO 'car' class id is 2
                bbox = xyxy[i].tolist()  # [x1, y1, x2, y2]
                score = float(conf_arr[i])
                cars.append({"bbox": bbox, "conf": score, "class_id": int(c), "name": names.get(c, str(c))})

    print(f"Found {len(cars)} car(s) (class id=2) with conf >= {conf}")
    for i, car in enumerate(cars, 1):
        x1, y1, x2, y2 = car["bbox"]
        print(f"#{i}: {car['name']} conf={car['conf']:.3f} bbox=[{x1:.1f}, {y1:.1f}, {x2:.1f}, {y2:.1f}]")

    # Save annotated image
    out_path = args.output
    if out_path is None:
        inp_name = Path(source).stem
        out_path = f"annotated_{inp_name}.jpg"

    try:
        annotated = res.plot()  # returns numpy array (H,W,3)
        im = Image.fromarray(annotated)
        im.save(out_path)
        print(f"Annotated image saved to: {out_path}")
    except Exception as e:
        print(f"Could not save annotated image: {e}")


if __name__ == "__main__":
    main()
