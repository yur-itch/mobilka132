import numpy as np
from prep import DigitRecognitionPrep

def prepare_full_dataset(x_mnist, y_mnist, x_custom, y_custom):
    print("Нормализация MNIST")
    
    prepared_mnist = []
    
    for i, row in enumerate(x_mnist):
        img = row.reshape(28, 28).astype(np.uint8)
        prep_img = DigitRecognitionPrep.prepare_image(img)
        prepared_mnist.append(prep_img.flatten())
        
        if i % 5000 == 0:
            print(f"Обработано {i} из {len(x_mnist)}")

    x_prepared_mnist = np.array(prepared_mnist)
    
    X = np.concatenate([x_prepared_mnist, x_custom], axis=0)
    Y = np.concatenate([y_mnist, y_custom], axis=0)
    indices = np.random.permutation(len(X))
    
    print(f"Итоговый размер датасета: {len(X)} примеров")
    return X[indices], Y[indices]

def save_ready_data(X, Y, filename="train_data_ready.npz"):
    np.savez_compressed(filename, x=X, y=Y)
    print(f"Данные упакованы в {filename}")

def load_ready_data(filename="train_data_ready.npz"):
    data = np.load(filename)
    return data['x'], data['y']