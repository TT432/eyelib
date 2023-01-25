/*    */ package software.bernie.geckolib3.core.molang.math;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Negate
/*    */   implements IValue
/*    */ {
/*    */   public IValue value;
/*    */   
/*    */   public Negate(IValue value) {
/* 12 */     this.value = value;
/*    */   }
/*    */ 
/*    */   
/*    */   public double get() {
/* 17 */     return (this.value.get() == 0.0D) ? 1.0D : 0.0D;
/*    */   }
/*    */ 
/*    */   
/*    */   public String toString() {
/* 22 */     return "!" + this.value.toString();
/*    */   }
/*    */ }


/* Location:              C:\Users\q2437\.gradle\caches\modules-2\files-2.1\com.eliotlash.mclib\mclib\20\3383672fb61b6a210e8fb93ffdd9fdae4549e4fc\mclib-20.jar!\com\eliotlash\mclib\math\Negate.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */