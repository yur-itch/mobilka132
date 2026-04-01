import numpy as np
import math

class DigitRecognitionPrep:
    
    @staticmethod
    def dilate(img, radius):
        h, w = img.shape
        result = np.zeros_like(img)
        
        kernel_size = radius * 2 + 1
        kernel = np.zeros((kernel_size, kernel_size), dtype=bool)
        cy, cx = radius, radius
        for i in range(kernel_size):
            for j in range(kernel_size):
                if math.sqrt((i - cy)**2 + (j - cx)**2) <= radius:
                    kernel[i, j] = True
                    
        y_indices, x_indices = np.nonzero(img > 0)
        for y, x in zip(y_indices, x_indices):
            y_min = max(0, y - radius)
            y_max = min(h, y + radius + 1)
            x_min = max(0, x - radius)
            x_max = min(w, x + radius + 1)
            
            ky_min = radius - (y - y_min)
            ky_max = radius + (y_max - y)
            kx_min = radius - (x - x_min)
            kx_max = radius + (x_max - x)
            
            result[y_min:y_max, x_min:x_max][kernel[ky_min:ky_max, kx_min:kx_max]] = 255
            
        return result

    @staticmethod
    def otsu_binarize(img):
        flat = img.flatten()
        histogram, _ = np.histogram(flat, bins=256, range=(0, 256))
        
        total = len(flat)
        sum_total = np.dot(np.arange(256), histogram)
        
        sum_b = 0.0
        w_b = 0
        max_variance = 0.0
        threshold = 0
        
        for t in range(256):
            w_b += histogram[t]
            if w_b == 0: continue
            w_f = total - w_b
            if w_f == 0: break
            
            sum_b += t * histogram[t]
            m_b = sum_b / w_b
            m_f = (sum_total - sum_b) / w_f
            
            variance_between = w_b * w_f * (m_b - m_f) ** 2
            
            if variance_between > max_variance:
                max_variance = variance_between
                threshold = t
                
        return np.where(img >= threshold, 255, 0).astype(np.uint8)

    @staticmethod
    def skeletonize(img):
        skel = (img > 0).astype(np.uint8)
        h, w = skel.shape
        
        def count_transitions(neighbors):
            transitions = 0
            for i in range(8):
                if neighbors[i] == 0 and neighbors[i+1] == 1:
                        transitions += 1
            return transitions

        changed = True
        while changed:
            changed = False
            
            to_remove = []
            for y in range(1, h - 1):
                for x in range(1, w - 1):
                    if skel[y, x] != 1: continue
                    p2, p3, p4 = skel[y-1, x], skel[y-1, x+1], skel[y, x+1]
                    p5, p6, p7 = skel[y+1, x+1], skel[y+1, x], skel[y+1, x-1]
                    p8, p9 = skel[y, x-1], skel[y-1, x-1]
                    
                    neighbors_sum = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9
                    if not (2 <= neighbors_sum <= 6): continue
                    
                    neighbors_arr = [p2, p3, p4, p5, p6, p7, p8, p9, p2]
                    if count_transitions(neighbors_arr) != 1: continue
                    
                    if p2 * p4 * p6 != 0: continue
                    if p4 * p6 * p8 != 0: continue
                    
                    to_remove.append((y, x))
                    
            if to_remove: changed = True
            for y, x in to_remove: skel[y, x] = 0
            
            to_remove = []
            for y in range(1, h - 1):
                for x in range(1, w - 1):
                    if skel[y, x] != 1: continue
                    p2, p3, p4 = skel[y-1, x], skel[y-1, x+1], skel[y, x+1]
                    p5, p6, p7 = skel[y+1, x+1], skel[y+1, x], skel[y+1, x-1]
                    p8, p9 = skel[y, x-1], skel[y-1, x-1]
                    
                    neighbors_sum = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9
                    if not (2 <= neighbors_sum <= 6): continue
                    
                    neighbors_arr = [p2, p3, p4, p5, p6, p7, p8, p9, p2]
                    if count_transitions(neighbors_arr) != 1: continue
                    
                    if p2 * p4 * p8 != 0: continue
                    if p2 * p6 * p8 != 0: continue
                    
                    to_remove.append((y, x))
                    
            if to_remove: changed = True
            for y, x in to_remove: skel[y, x] = 0
            
        return skel * 255

    @staticmethod
    def crop_bounding_box(img):
        coords = np.argwhere(img > 0)
        if coords.size == 0:
            return img
        y_min, x_min = coords.min(axis=0)
        y_max, x_max = coords.max(axis=0)
        return img[y_min:y_max+1, x_min:x_max+1]

    @staticmethod
    def center_of_mass(img):
        coords = np.argwhere(img > 0)
        if coords.size == 0:
            return img.shape[1] / 2.0, img.shape[0] / 2.0
        y_center, x_center = coords.mean(axis=0)
        return x_center, y_center

    @staticmethod
    def resize_image(img, target_w, target_h):
        h, w = img.shape
        x_indices = np.clip(np.int_(np.linspace(0, w - 1, target_w)), 0, w - 1)
        y_indices = np.clip(np.int_(np.linspace(0, h - 1, target_h)), 0, h - 1)
        return img[np.ix_(y_indices, x_indices)]

    @staticmethod
    def scale_and_center(img):
        h, w = img.shape
        max_dim = max(w, h)
        scale = 20.0 / max(1, max_dim)
        
        nw = max(1, int(w * scale))
        nh = max(1, int(h * scale))
        
        resized = DigitRecognitionPrep.resize_image(img, nw, nh)
        cx, cy = DigitRecognitionPrep.center_of_mass(resized)
        
        shift_x = int(14.0 - cx)
        shift_y = int(14.0 - cy)
        
        canvas = np.zeros((28, 28), dtype=np.uint8)
        
        start_y = max(0, shift_y)
        end_y = min(28, shift_y + nh)
        start_x = max(0, shift_x)
        end_x = min(28, shift_x + nw)
        
        img_start_y = max(0, -shift_y)
        img_end_y = img_start_y + (end_y - start_y)
        img_start_x = max(0, -shift_x)
        img_end_x = img_start_x + (end_x - start_x)
        
        canvas[start_y:end_y, start_x:end_x] = resized[img_start_y:img_end_y, img_start_x:img_end_x]
        return canvas

    @staticmethod
    def gaussian_blur(img, radius, sigma):
        kernel_size = 2 * radius + 1
        kernel = np.zeros(kernel_size)
        sigma2 = 2 * sigma * sigma
        
        for i in range(-radius, radius + 1):
            kernel[i + radius] = math.exp(-(i * i) / sigma2)
        kernel /= np.sum(kernel)
        
        temp = np.zeros_like(img, dtype=float)
        h, w = img.shape
        for y in range(h):
            for x in range(w):
                val = 0.0
                for k in range(-radius, radius + 1):
                    xn = max(0, min(w - 1, x + k))
                    val += img[y, xn] * kernel[k + radius]
                temp[y, x] = val
                
        result = np.zeros_like(img, dtype=np.uint8)
        for y in range(h):
            for x in range(w):
                val = 0.0
                for k in range(-radius, radius + 1):
                    yn = max(0, min(h - 1, y + k))
                    val += temp[yn, x] * kernel[k + radius]
                result[y, x] = min(255, max(0, int(val)))
                
        return result

    @classmethod
    def prepare_image(cls, img_array):
        res = cls.crop_bounding_box(img_array)
        
        max_dim = max(res.shape)
        scale = 50.0 / max_dim
        res = cls.resize_image(res, max(1, int(res.shape[1] * scale)), max(1, int(res.shape[0] * scale)))
        
        res = cls.otsu_binarize(res)
        res = np.pad(res, pad_width=1, mode='constant', constant_values=0)
        res = cls.skeletonize(res)
        res = cls.dilate(res, 2)
        res = cls.scale_and_center(res)
        res = cls.gaussian_blur(res, 1, 0.5)
        
        return res.astype(np.float32) / 255.0