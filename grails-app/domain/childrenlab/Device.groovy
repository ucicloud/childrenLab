package childrenlab

class Device {

    static belongsTo = [kid: Kids]
    String swingVersion
    String macId
    String batteryStatus

    static constraints = {
        swingVersion nullable: true
        kid nullable: true
        macId nullable: false, unique: true
        batteryStatus nullable: true
    }
}