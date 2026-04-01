import numpy as np

def relu(x):
    return np.maximum(0, x)

def relu_derivative(x):
    return (x > 0).astype(float)

def softmax(x):
    exps = np.exp(x - np.max(x, axis=1, keepdims=True))
    return exps / np.sum(exps, axis=1, keepdims=True)

def init_layer(in_dim, out_dim):
    limit = np.sqrt(2. / in_dim)
    weights = np.random.randn(in_dim, out_dim) * limit
    biases = np.zeros((1, out_dim))
    return weights, biases

def forward(x, weights, biases):
    w1, b1 = weights[0], biases[0]
    w2, b2 = weights[1], biases[1]
    w3, b3 = weights[2], biases[2]
    z1 = x @ w1 + b1
    a1 = relu(z1)
    z2 = a1 @ w2 + b2
    a2 = relu(z2)
    z3 = a2 @ w3 + b3
    a3 = softmax(z3)
    cache = (x, z1, a1, z2, a2, z3, a3)
    return a3, cache

def backward(y_true, cache, weights):
    x, z1, a1, z2, a2, z3, a3 = cache
    w1, w2, w3 = weights
    batch_size = x.shape[0]
    y_oh = np.zeros_like(a3)
    y_oh[np.arange(batch_size), y_true] = 1
    dz3 = (a3 - y_oh) / batch_size
    dw3 = a2.T @ dz3
    db3 = np.sum(dz3, axis=0, keepdims=True)
    da2 = dz3 @ w3.T
    dz2 = da2 * relu_derivative(z2)
    dw2 = a1.T @ dz2
    db2 = np.sum(dz2, axis=0, keepdims=True)
    da1 = dz2 @ w2.T
    dz1 = da1 * relu_derivative(z1)
    dw1 = x.T @ dz1
    db1 = np.sum(dz1, axis=0, keepdims=True)
    
    return (dw1, db1), (dw2, db2), (dw3, db3)