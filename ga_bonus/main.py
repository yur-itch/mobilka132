import random
import copy
import seed


NUM_REGS = 3
POP_SIZE = 1000
GENERATIONS = 0xffffffffffffffff
OP_LIMIT = 500 
INITIAL_BRANCH_CHANCE = 1.0
DECAY_FACTOR = 0.75


FIB_TARGETS = [0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55]

class EvalContext:
    def __init__(self, op_limit):
        self.ops_left = op_limit

    def consume(self, amount=1):
        self.ops_left -= amount
        if self.ops_left <= 0:
            raise TimeoutError("Limit exceeded")

class Node:
    def to_c(self): pass

    def eval(self, r, ctx, n_val): pass

class ConstNode(Node):
    def __init__(self, val=1): self.val = val

    def to_c(self): return str(self.val)

    def eval(self, r, ctx, n_val):
        ctx.consume(1)
        return float(self.val)

class RegNode(Node):
    def __init__(self, idx): self.idx = idx % NUM_REGS
    def to_c(self): return f"r[{self.idx}]"
    def eval(self, r, ctx, n_val):
        ctx.consume(1)
        return float(r[self.idx])

class BinaryOpNode(Node):
    def __init__(self, op, idx, right):
        self.op, self.idx, self.right = op, idx % NUM_REGS, right

    def to_c(self):
        ops = {"set": "=", "add": "+=", "sub": "-="}
        return f"(r[{self.idx}] {ops[self.op]} ({self.right.to_c()}))"
    
    def eval(self, r, ctx, n_val):
        ctx.consume(1)
        val = self.right.eval(r, ctx, n_val)
        if self.op == "set": r[self.idx] = val
        elif self.op == "add": r[self.idx] += val
        elif self.op == "sub": r[self.idx] -= val
        return r[self.idx]

class BlockNode(Node):
    def __init__(self, nodes): self.nodes = nodes

    def to_c(self):
        return "(" + " , ".join([n.to_c() for n in self.nodes]) + ")"
    
    def eval(self, r, ctx, n_val):
        res = 0
        for node in self.nodes:
            res = node.eval(r, ctx, n_val)
        return res

class LoopNode(Node):
    def __init__(self, left_body, right_body):
        self.left_body, self.right_body = left_body, right_body

    def to_c(self):
        return (f"({{ long long last = 0; for(int i=0; i<n; i++) {{ "
                f"{self.left_body.to_c()}; last = {self.right_body.to_c()}; }} last; }})")
    
    def eval(self, r, ctx, n_val):
        last_val = 0
        for _ in range(max(0, min(n_val, 100))):
            ctx.consume(1)
            self.left_body.eval(r, ctx, n_val)
            last_val = self.right_body.eval(r, ctx, n_val)
        return last_val

def get_all_nodes(node):
    nodes = [node]
    if isinstance(node, BinaryOpNode):
        nodes.extend(get_all_nodes(node.right))
    elif isinstance(node, BlockNode):
        for n in node.nodes: nodes.extend(get_all_nodes(n))
    elif isinstance(node, LoopNode):
        nodes.extend(get_all_nodes(node.left_body))
        nodes.extend(get_all_nodes(node.right_body))
    return nodes

def replace_subtree(root, target, replacement):
    if root is target: return replacement
    if isinstance(root, BinaryOpNode):
        root.right = replace_subtree(root.right, target, replacement)
    elif isinstance(root, BlockNode):
        root.nodes = [replace_subtree(n, target, replacement) for n in root.nodes]
    elif isinstance(root, LoopNode):
        root.left_body = replace_subtree(root.left_body, target, replacement)
        root.right_body = replace_subtree(root.right_body, target, replacement)
    return root

def generate_node(depth=0):
    chance = INITIAL_BRANCH_CHANCE * (DECAY_FACTOR ** depth)
    if random.random() > chance:
        return random.choice([
            RegNode(random.randint(0, NUM_REGS - 1)),
            ConstNode(random.choice([0, 1]))
        ])

    choice = random.choice(["set", "add", "sub", "block", "loop", "reg", "const"])

    if choice in ["set", "add", "sub"]:
        return BinaryOpNode(
            choice,
            random.randint(0, NUM_REGS - 1),
            generate_node(depth + 1)
        )
    elif choice == "block":
        return BlockNode([generate_node(depth + 1) for _ in range(random.randint(2, 4))])
    elif choice == "loop":
        return LoopNode(generate_node(depth + 1), generate_node(depth + 1))
    elif choice == "const":
        return ConstNode(random.choice([0, 1]))
    return RegNode(random.randint(0, NUM_REGS - 1))

def mutate(node, depth=0):
    if random.random() < 0.1: return generate_node(depth)
    if isinstance(node, ConstNode):
        if random.random() < 0.1:
            node.val = random.choice([0, 1])
    if isinstance(node, BinaryOpNode):
        if random.random() < 0.1: node.idx = random.randint(0, NUM_REGS - 1)
        if random.random() < 0.1: node.op = random.choice(["set", "add", "sub"])
        node.right = mutate(node.right, depth + 1)
    elif isinstance(node, BlockNode):
        node.nodes = [mutate(n, depth + 1) for n in node.nodes]
        if random.random() < 0.1:
            node.nodes.append(generate_node(depth + 1))
    elif isinstance(node, LoopNode):
        node.left_body = mutate(node.left_body, depth + 1)
        node.right_body = mutate(node.right_body, depth + 1)
    return node

def run_crossover(p1, p2):
    child = copy.deepcopy(p1)
    n1, n2 = get_all_nodes(child), get_all_nodes(p2)
    target = random.choice(n1)
    replacement = copy.deepcopy(random.choice(n2))
    return replace_subtree(child, target, replacement)

def evaluate_fitness(node):
    err = 0.0
    try:
        for n, target in enumerate(FIB_TARGETS):
            ctx = EvalContext(OP_LIMIT)
            r = [0.0, 0.0, 0.0]
            res = node.eval(r, ctx, n)
            err += abs(res - target)
    except Exception:
        return 1e12
    return err

def run_evolution():
    pop = [generate_node(0) for _ in range(POP_SIZE)]
    history = []

    for gen in range(GENERATIONS):
        scored = sorted([(evaluate_fitness(p), p) for p in pop], key=lambda x: x[0])
        best_f, best_p = scored[0]
        
        print(f"Gen {gen:3} | Fitness: {best_f:<8.2f} | Code: {best_p.to_c()[:70]}...")
        if best_f < 0.5: break

        history.append(best_f)
        if len(history) > 30 and len(set(history[-30:])) == 1:
            print("Regenerating everything besides the elites")
            pop = [s[1] for s in scored[:5]] + [generate_node(0) for _ in range(POP_SIZE-5)]
            history = []
            continue

        elite = [s[1] for s in scored[:50]]
        next_gen = copy.deepcopy(elite[:10])
        
        while len(next_gen) < POP_SIZE:
            r = random.random()
            if r < 0.3:
                next_gen.append(mutate(copy.deepcopy(random.choice(elite))))
            elif r < 0.8:
                p1, p2 = random.sample(elite, 2)
                next_gen.append(mutate(run_crossover(p1, p2)))
            else:
                next_gen.append(generate_node(0))
        pop = next_gen
    return best_p

if __name__ == "__main__":
    random.seed(seed.random())
    final_boss = run_evolution()
    out = "fib.c"
    with open(out, "w") as f:
        f.write(f"#include <stdio.h>\nlong long f(int n){{long long r[3]={{0,0,0}}; return {final_boss.to_c()};}}\n")
        f.write("int main(){for(int i=0;i<=10;i++)printf(\"%d: %lld\\n\",i,f(i));}")
    print("\nReady in", out)
