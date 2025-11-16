import argparse
from pathlib import Path

from ultralytics import YOLO


def main():
    parser = argparse.ArgumentParser(description="Export YOLO model to TFLite for Android")
    parser.add_argument("--weights", type=Path, default=Path("yolo11n.pt"), help="Path to .pt weights")
    parser.add_argument("--imgsz", type=int, default=640, help="Export image size (square)")
    parser.add_argument("--int8", action="store_true", help="Export int8 quantized TFLite (requires calibration)")
    parser.add_argument("--out", type=Path, default=Path("yolo11n.tflite"), help="Output tflite path")
    args = parser.parse_args()

    model = YOLO(str(args.weights))
    print(f"Exporting {args.weights} -> TFLite (@{args.imgsz}) ...")
    exported = model.export(
        format="tflite",
        imgsz=args.imgsz,
        nms=True,  # include NMS in the graph to simplify Android side
        int8=args.int8,
    )
    # Ultralytics returns path to exported file
    exp_path = Path(exported)
    print(f"Exported to: {exp_path}")
    if args.out and exp_path.exists() and args.out.resolve() != exp_path.resolve():
        args.out.write_bytes(exp_path.read_bytes())
        print(f"Copied to: {args.out}")


if __name__ == "__main__":
    main()
