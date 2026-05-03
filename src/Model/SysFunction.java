package Model;

import java.sql.Date;

/** Maps to SYS_FUNCTION table */
public class SysFunction {
    private int    functionId;
    private String nameFunction;
    private Date   createdAt;
    private Date   updatedAt;

    public SysFunction() {}

    public SysFunction(int functionId, String nameFunction, Date createdAt, Date updatedAt) {
        this.functionId   = functionId;
        this.nameFunction = nameFunction;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    public SysFunction(String nameFunction) {
        this.nameFunction = nameFunction;
    }

    public int    getFunctionId()              { return functionId; }
    public void   setFunctionId(int v)         { this.functionId = v; }
    public String getNameFunction()            { return nameFunction; }
    public void   setNameFunction(String v)    { this.nameFunction = v; }
    public Date   getCreatedAt()               { return createdAt; }
    public void   setCreatedAt(Date v)         { this.createdAt = v; }
    public Date   getUpdatedAt()               { return updatedAt; }
    public void   setUpdatedAt(Date v)         { this.updatedAt = v; }

    @Override public String toString() { return nameFunction; }
}
