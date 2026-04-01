import numpy as np
import os
from prep import DigitRecognitionPrep
from model import init_layer, forward, backward
EPOCHS = 50
BATCH_SIZE = 128
LEARNING_RATE = 0.01
HIDDEN_1 = 256
HIDDEN_2 = 128
OUTPUT_DIR = "model_weights_csv"
MNIST_CACHE = "mnist_processed.npz"

def load_mnist_csv(path):
    if not os.path.exists(path):
        raise FileNotFoundError(f"Файл {path} не найден")
    print(f"Читаю {path}...")
    data = np.loadtxt(path, delimiter=',', skiprows=1)
    labels = data[:, 0].astype(int)
    pixels = data[:, 1:]
    return pixels, labels

def main():
    if not os.path.exists(MNIST_CACHE):
        print("Обработка MNIST")
        x_raw, y_raw = load_mnist_csv('mnist_train.csv')
        
        processed_mnist = []
        for i, row in enumerate(x_raw):
            img = row.reshape(28, 28).astype(np.uint8)
            prep_img = DigitRecognitionPrep.prepare_image(img)
            processed_mnist.append(prep_img.flatten())
            
            if i % 5000 == 0:
                print(f"Обработано MNIST: {i}/{len(x_raw)}")
        
        x_mnist = np.array(processed_mnist, dtype=np.float32)
        y_mnist = y_raw
        np.savez_compressed(MNIST_CACHE, x=x_mnist, y=y_mnist)
        print("Кэш MNIST создан.")
    else:
        print("Загрузка MNIST из кэша")
        data = np.load(MNIST_CACHE)
        x_mnist, y_mnist = data['x'], data['y']
    print("Загрузка пользовательских данных")
    if os.path.exists("my_x.npy") and os.path.exists("my_y.npy"):
        x_custom = np.load("my_x.npy")
        y_custom = np.load("my_y.npy")
        print(f"Добавлено {len(x_custom)} собственных примеров.")
    else:
        print("Файлы my_x.npy/my_y.npy не найдены")
        x_custom = np.empty((0, 784))
        y_custom = np.empty((0,), dtype=int)
    X = np.concatenate([x_mnist, x_custom], axis=0)
    Y = np.concatenate([y_mnist, y_custom], axis=0)
    
    indices = np.random.permutation(len(X))
    X, Y = X[indices], Y[indices]
    print("Инициализация сети")
    w1, b1 = init_layer(784, HIDDEN_1)
    w2, b2 = init_layer(HIDDEN_1, HIDDEN_2)
    w3, b3 = init_layer(HIDDEN_2, 10)
    
    weights = [w1, w2, w3]
    biases = [b1, b2, b3]
    print(f"Начинаю обучение: {len(X)} примеров, {EPOCHS} эпох.")
    
    for epoch in range(EPOCHS):
        p = np.random.permutation(len(X))
        X_s, Y_s = X[p], Y[p]
        
        epoch_loss = 0
        for i in range(0, len(X), BATCH_SIZE):
            x_batch = X_s[i : i + BATCH_SIZE]
            y_batch = Y_s[i : i + BATCH_SIZE]
            probs, cache = forward(x_batch, weights, biases)
            batch_sz = x_batch.shape[0]
            log_probs = -np.log(probs[np.arange(batch_sz), y_batch] + 1e-8)
            epoch_loss += np.sum(log_probs)
            g1, g2, g3 = backward(y_batch, cache, weights)
            weights[0] -= LEARNING_RATE * g1[0]
            biases[0] -= LEARNING_RATE * g1[1]
            weights[1] -= LEARNING_RATE * g2[0]
            biases[1] -= LEARNING_RATE * g2[1]
            weights[2] -= LEARNING_RATE * g3[0]
            biases[2] -= LEARNING_RATE * g3[1]
            
        print(f"Эпоха {epoch + 1}/{EPOCHS} | Средний Loss: {epoch_loss/len(X):.4f}")
    print(f"Экспорт весов в папку {OUTPUT_DIR}")
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
        
    for i, (W, B) in enumerate(zip(weights, biases)):
        layer_idx = i + 1
        np.savetxt(f"{OUTPUT_DIR}/layer_{layer_idx}_weights.csv", W.T, delimiter=",")
        np.savetxt(f"{OUTPUT_DIR}/layer_{layer_idx}_biases.csv", B.flatten(), delimiter=",")
    
    print("Готово")

if __name__ == "__main__":
    main()