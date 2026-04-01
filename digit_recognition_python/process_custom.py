import os
import numpy as np
from PIL import Image
from prep import DigitRecognitionPrep

def process_folders(base_path):
    x_data = []
    y_data = []
    for digit in range(10):
        digit_dir = os.path.join(base_path, str(digit))
        
        if not os.path.exists(digit_dir):
            print(f"Папка {digit_dir} не найдена")
            continue
            
        print(f"Обработка цифры {digit}")
        for filename in os.listdir(digit_dir):
            if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp')):
                file_path = os.path.join(digit_dir, filename)
                
                try:
                    with Image.open(file_path) as img:
                        img_gray = img.convert('L')
                        img_array = np.array(img_gray)
                        prepared = DigitRecognitionPrep.prepare_image(img_array)
                        x_data.append(prepared.flatten())
                        y_data.append(digit)
                except Exception as e:
                    print(f"Ошибка при чтении {filename}: {e}")

    return np.array(x_data, dtype=np.float32), np.array(y_data, dtype=np.int32)

if __name__ == "__main__":
    SOURCE_PATH = "custom_digits" 
    
    X, Y = process_folders(SOURCE_PATH)
    
    if len(X) > 0:
        np.save("my_x.npy", X)
        np.save("my_y.npy", Y)
        print(f"\nГотово. Обработано {len(X)} изображений.")
        print("Файлы 'my_x.npy' и 'my_y.npy' созданы.")
    else:
        print("Изображения не найдены")