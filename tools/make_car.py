"""
Генерирует client/CarModel.java из tools/car_model.json (коробки модели ВАЗ-2109 ППС,
извлечённые из мода [B'A'D] Russian Police Pack). Текстура: textures/entity/police_car.png (288x288).
Модель центрируется по x/z и ставится колёсами на землю (y=24).
"""
import json, os
HERE = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(HERE, "car_model.json")))
boxes, pivot, rot = d["boxes"], d["pivot"], d["rot"]

# границы (с учётом поворотов только вокруг X) для центровки
import math
def extent():
    xs, ys, zs = [], [], []
    for part, v in boxes:
        u, vv, x, y, z, w, h, dd = v[:8]
        o = [pivot["bb_main"][0], pivot["bb_main"][1], pivot["bb_main"][2]]
        if part != "bb_main":
            o = [o[i] + pivot[part][i] for i in range(3)]
            a = rot[part][0]; c, s = math.cos(a), math.sin(a)
        else:
            c, s = 1, 0
        for X in (x, x + w):
            for Y in (y, y + h):
                for Z in (z, z + dd):
                    yy = Y * c - Z * s; zz = Y * s + Z * c
                    xs.append(o[0] + X); ys.append(o[1] + yy); zs.append(o[2] + zz)
    return min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)
x0, x1, y0, y1, z0, z1 = extent()
shift = (-(x0 + x1) / 2.0, 24.0 - y1, -(z0 + z1) / 2.0)
px, py, pz = (pivot["bb_main"][0] + shift[0], pivot["bb_main"][1] + shift[1], pivot["bb_main"][2] + shift[2])

def f(x):
    return ("%g" % x) + "F"

def builder(part):
    out = ["CubeListBuilder.create()"]
    for p, v in boxes:
        if p != part: continue
        u, vv, x, y, z, w, h, dd = v[:8]
        out.append("            .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (u, vv, f(x), f(y), f(z), f(w), f(h), f(dd)))
    return "\n".join(out)

L = ["package babushka.client;", "",
     "import net.minecraft.client.model.EntityModel;",
     "import net.minecraft.client.model.geom.ModelPart;",
     "import net.minecraft.client.model.geom.PartPose;",
     "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
     "import net.minecraft.client.model.geom.builders.LayerDefinition;",
     "import net.minecraft.client.model.geom.builders.MeshDefinition;",
     "import net.minecraft.client.model.geom.builders.PartDefinition;",
     "import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;", "",
     "/**",
     " * ВАЗ-2109 ППС. ГЕНЕРИРУЕТСЯ tools/make_car.py из модели мода [B'A'D] Russian Police Pack",
     " * (только внешний вид, никакой логики машины).",
     " */",
     "public class CarModel extends EntityModel<LivingEntityRenderState> {",
     "    public CarModel(ModelPart root) {",
     "        super(root);",
     "    }", "",
     "    public static LayerDefinition createBodyLayer() {",
     "        MeshDefinition mesh = new MeshDefinition();",
     "        PartDefinition root = mesh.getRoot();", "",
     '        PartDefinition bb_main = root.addOrReplaceChild("bb_main",',
     "            " + builder("bb_main") + ",",
     "            PartPose.offset(%s, %s, %s));" % (f(px), f(py), f(pz)), ""]
for name in sorted(k for k in pivot if k != "bb_main"):
    po = pivot[name]; r = rot[name]
    L.append('        bb_main.addOrReplaceChild("%s",\n            %s,\n            PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));\n' % (
        name, builder(name), f(po[0]), f(po[1]), f(po[2]), f(r[0]), f(r[1]), f(r[2])))
L += ["        return LayerDefinition.create(mesh, 288, 288);", "    }", "}", ""]
open(os.path.join(HERE, "..", "src/main/java/babushka/client/CarModel.java"), "w", encoding="utf-8").write("\n".join(L))
print("центр сдвинут на", shift, "размер: длина %.0f, ширина %.0f, высота %.0f юнитов" % (z1 - z0, x1 - x0, y1 - y0))
