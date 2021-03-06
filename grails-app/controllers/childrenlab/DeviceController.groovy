package childrenlab

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional

@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class DeviceController {
    def deviceService

    def uploadData(String x, String y, String z, String u, String v, String macId){
        def result = deviceService.uploadData(x, y, z, u, v, macId)

        render result as JSON
    }

    def uploadRawData(String indoorActivity, String outdoorActivity, long time, String macId, int timezone){
        def result = deviceService.uploadRawData(indoorActivity, outdoorActivity, time, macId, timezone)

        render result as JSON
    }

    def getYearlyActivity(String macId) {
        def result = deviceService.getYearlyActivity(macId)

        render result as JSON
    }

    def getMonthlyActivity(String macId){
        def result = deviceService.getMonthlyActivity(macId)

        render result as JSON
    }
    
    def getWeeklyActivity(String macId){
        def result = deviceService.getWeeklyActivity(macId)

        render result as JSON
    }

    def getDailyActivity(String macId){
        def result = deviceService.getDailyActivity(macId)

        render result as JSON
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TESTER'])
    def list(){
        def device = Device.list()

        render(view: "list", model: [deviceList: device, deviceInstanceCount: Device.count()])
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TESTER'])
    def deviceDataList(String macId, int userId, int max){
        def device = Device.findByMacIdAndUser(macId, User.get(userId))

        params.max = Math.min(max ?: 50, 100)
        def c = Activity.createCriteria()
        def data = c.list(params) {
            eq('device', device)
            order('receivedTime', "desc")
        }

        render(view: "dataList", model: [data: data, deviceActivityInstance: Activity.countByDevice(device), macId: macId, userId: userId])
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TESTER'])
    def deviceDataRawList(String macId, int userId, int max){
        def device = Device.findByMacIdAndUser(macId, User.get(userId))

        params.max = Math.min(max ?: 50, 100)
        def c = ActivityRaw.createCriteria()
        def data = c.list(params) {
            eq('device', device)
            order('time', "desc")
        }

        render(view: "dataRawList", model: [data: data, deviceActivityInstanceCount: ActivityRaw.countByDevice(device), macId: macId, userId: userId])
    }

    def export(String macId, int userId){
        def device = Device.findByMacIdAndUser(macId, User.get(userId))
        def data = DeviceActivity.findAllByDevice(device)

        String csvFile = "Activity X, Activity Y, Activity Z, U, V, Date\n"
        data.each(){
            def date = new Date(it.receivedTime*1000).format("YYYY/MM/dd HH:mm:ss")
            csvFile += "${it.activityX}, ${it.activityY}, ${it.activityZ}, ${it.u}, ${it.v}, ${date}\n"
        }

        response.setHeader("content-disposition", "attachment; filename=Activity_${macId}.csv")
        render(contentType: "text/csv", text: csvFile)
    }

    def exportRaw(String macId, int userId){
        def device = Device.findByMacIdAndUser(macId, User.get(userId))
        def data = ActivityRaw.findAllByDevice(device)

        String csvFile = "Indoor, Outdoor, Time, Mac ID\n"
        data.each(){
            def date = new Date(it.time*1000).format("YYYY/MM/dd HH:mm:ss")
            csvFile += "${it.indoorActivity}, ${it.outdoorActivity}, ${date}, ${device.macId}\n"
        }

        response.setHeader("content-disposition", "attachment; filename=Activity_${macId}.csv")
        render(contentType: "text/csv", text: csvFile)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TESTER'])
    @Transactional
    def deleteByDevice(String macId){
        if(!macId){
            render 'Wrong'
            return;
        }

        def device = Device.findByMacId(macId)
        def delete = DeviceActivity.executeUpdate("delete from DeviceActivity where device = ?", [device])

        redirect([action: 'list'])
    }
}
