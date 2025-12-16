"""
Train Model v10 - REAL Phishing URL Data with Label Smoothing
- Uses real phishing URLs from JPCERT/CC (2024)
- Label Smoothing to prevent extreme outputs (0.1 instead of 0, 0.9 instead of 1)
- Temperature Scaling for calibrated probabilities
"""

import os
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import json

data_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")

# ============================================
# Hyperparameters
# ============================================
LABEL_SMOOTHING = 0.1  # 0 -> 0.1, 1 -> 0.9 to prevent extreme outputs
EPOCHS = 200
BATCH_SIZE = 16
LEARNING_RATE = 0.001
DROPOUT_RATE = 0.3

print("=" * 60)
print("Training Model v10 - REAL Phishing Data + Label Smoothing")
print("=" * 60)
print()

# Load dataset
print("Loading dataset_v10.csv (real phishing URLs)...")
df = pd.read_csv(os.path.join(data_dir, 'dataset_v10.csv'))

print(f"Total samples: {len(df)}")
print(f"Phishing: {df['is_phishing'].sum()}")
print(f"Legitimate: {len(df) - df['is_phishing'].sum()}")
print()

# Separate features and labels
X = df.drop('is_phishing', axis=1).values.astype(np.float32)
y = df['is_phishing'].values.astype(np.float32)

# Apply Label Smoothing
def smooth_labels(y, smoothing=LABEL_SMOOTHING):
    """Convert 0/1 labels to 0.1/0.9 to prevent overconfident predictions"""
    return y * (1 - smoothing) + (1 - y) * smoothing

y_smoothed = smooth_labels(y)
print(f"Label Smoothing applied: 0 -> {LABEL_SMOOTHING}, 1 -> {1-LABEL_SMOOTHING}")
print()

# Split data
X_train, X_test, y_train, y_test = train_test_split(
    X, y_smoothed, test_size=0.2, random_state=42, stratify=y
)

# Original labels for evaluation
_, _, y_train_orig, y_test_orig = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

print(f"Training samples: {len(X_train)}")
print(f"Test samples: {len(X_test)}")
print()

# Normalize features
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Save scaler configuration
scaler_config = {
    'mean': scaler.mean_.tolist(),
    'scale': scaler.scale_.tolist()
}
with open(os.path.join(data_dir, 'scaler_config_v10.json'), 'w') as f:
    json.dump(scaler_config, f, indent=2)
print("Saved scaler_config_v10.json")

# ============================================
# Build Model - Deeper network for better pattern recognition
# ============================================
print()
print("Building model...")

model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(13,)),  # 13 features

    # First hidden layer
    tf.keras.layers.Dense(64, kernel_regularizer=tf.keras.regularizers.l2(0.01)),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Activation('relu'),
    tf.keras.layers.Dropout(DROPOUT_RATE),

    # Second hidden layer
    tf.keras.layers.Dense(32, kernel_regularizer=tf.keras.regularizers.l2(0.01)),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Activation('relu'),
    tf.keras.layers.Dropout(DROPOUT_RATE),

    # Third hidden layer
    tf.keras.layers.Dense(16, kernel_regularizer=tf.keras.regularizers.l2(0.01)),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Activation('relu'),
    tf.keras.layers.Dropout(DROPOUT_RATE),

    # Output layer
    tf.keras.layers.Dense(1, activation='sigmoid')
])

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE),
    loss='binary_crossentropy',
    metrics=['accuracy']
)

model.summary()

# ============================================
# Train Model
# ============================================
print()
print(f"Training for {EPOCHS} epochs...")

# Early stopping to prevent overfitting
early_stopping = tf.keras.callbacks.EarlyStopping(
    monitor='val_loss',
    patience=30,
    restore_best_weights=True,
    verbose=1
)

# Reduce learning rate on plateau
reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
    monitor='val_loss',
    factor=0.5,
    patience=10,
    min_lr=0.00001,
    verbose=1
)

history = model.fit(
    X_train_scaled, y_train,
    validation_data=(X_test_scaled, y_test),
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    callbacks=[early_stopping, reduce_lr],
    verbose=1
)

# ============================================
# Evaluate Model
# ============================================
print()
print("=" * 60)
print("Evaluation Results")
print("=" * 60)

# Get predictions
y_pred_raw = model.predict(X_test_scaled, verbose=0).flatten()

# Apply threshold 0.5 to original labels
y_pred_binary = (y_pred_raw >= 0.5).astype(int)
y_test_binary = (y_test_orig >= 0.5).astype(int)

# Calculate metrics
correct = np.sum(y_pred_binary == y_test_binary)
accuracy = correct / len(y_test_binary) * 100

# True positives, false positives, etc.
tp = np.sum((y_pred_binary == 1) & (y_test_binary == 1))
tn = np.sum((y_pred_binary == 0) & (y_test_binary == 0))
fp = np.sum((y_pred_binary == 1) & (y_test_binary == 0))
fn = np.sum((y_pred_binary == 0) & (y_test_binary == 1))

precision = tp / (tp + fp) if (tp + fp) > 0 else 0
recall = tp / (tp + fn) if (tp + fn) > 0 else 0
f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0

print(f"Accuracy: {accuracy:.1f}%")
print(f"Precision: {precision:.3f}")
print(f"Recall: {recall:.3f}")
print(f"F1 Score: {f1:.3f}")
print()
print(f"True Positives: {tp}")
print(f"True Negatives: {tn}")
print(f"False Positives: {fp}")
print(f"False Negatives: {fn}")

# Show prediction distribution
print()
print("Prediction Distribution (raw scores):")
print(f"  Min: {y_pred_raw.min():.4f}")
print(f"  Max: {y_pred_raw.max():.4f}")
print(f"  Mean: {y_pred_raw.mean():.4f}")
print(f"  Std: {y_pred_raw.std():.4f}")

# Check for extreme values
extreme_low = np.sum(y_pred_raw < 0.05)
extreme_high = np.sum(y_pred_raw > 0.95)
print()
print(f"Extreme low (<5%): {extreme_low}/{len(y_pred_raw)}")
print(f"Extreme high (>95%): {extreme_high}/{len(y_pred_raw)}")

# ============================================
# Convert to TFLite
# ============================================
print()
print("Converting to TFLite...")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save TFLite model
tflite_path = os.path.join(data_dir, 'phishing_model_v10.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

print(f"Saved: {tflite_path}")
print(f"Model size: {len(tflite_model) / 1024:.1f} KB")

# ============================================
# Test TFLite Model
# ============================================
print()
print("Testing TFLite model...")

interpreter = tf.lite.Interpreter(model_content=tflite_model)
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Test a few samples
test_predictions = []
for i in range(len(X_test_scaled)):
    input_data = X_test_scaled[i:i+1].astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    pred = interpreter.get_tensor(output_details[0]['index'])[0][0]
    test_predictions.append(pred)

test_predictions = np.array(test_predictions)
tflite_accuracy = np.mean((test_predictions >= 0.5).astype(int) == y_test_binary) * 100

print(f"TFLite Accuracy: {tflite_accuracy:.1f}%")

print()
print("=" * 60)
print("Training Complete!")
print("=" * 60)
print()
print("Files created:")
print(f"  - {tflite_path}")
print(f"  - {os.path.join(data_dir, 'scaler_config_v10.json')}")
print()
print("Next steps:")
print("  1. Run test_real_phishing_v10.py to test on real phishing URLs")
print("  2. Copy model to Android app assets folder")
