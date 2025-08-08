from deepface import DeepFace
import cv2

# Start webcam
cap = cv2.VideoCapture(0)

while True:
    ret, frame = cap.read()
    if not ret:
        break

    try:
        # Analyze face for emotions
        result = DeepFace.analyze(frame, actions=['emotion'], enforce_detection=False)

        # DeepFace returns a list of results, we take the first
        emotion = result[0]['dominant_emotion']

        # Map emotions to "frown" or "not frown"
        if emotion in ['angry', 'sad', 'disgust']:
            label = "Frown"
            color = (0, 0, 255)  # Red
        else:
            label = "Not Frown"
            color = (0, 255, 0)  # Green

        # Draw label on frame
        cv2.putText(frame, f"{label} ({emotion})", (50, 50),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, color, 2, cv2.LINE_AA)
    except Exception as e:
        print("No face detected")

    cv2.imshow("Frown Detector", frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
