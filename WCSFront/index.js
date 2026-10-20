


let resultJson = document.querySelector("#showResult")


function getCity(objEnviar){
    fetch("http://localhost:8080/search",{
        method: "GET",
        headers: {
            "Content-Type":"application/json",
            "Access-Control-Allow-Origin":"*"
        },
        body: JSON.stringify(objEnviar)
    })
    .then(response =>{console.log("response",response.json())})
    .then(data =>{console.log(data)})
    .catch(err =>{console.log("error", err)})

}

function postCity(objEnviar){
    fetch("http://localhost:8080/search",{
        method: "POST",
        headers: {
            "Content-Type":"application/json",
            "Access-Control-Allow-Origin":"*"
        },
        body: JSON.stringify(objEnviar)
    })
    .then(response =>{console.log("response: ", response.json)})
    .then(data =>{console.log("data: ", data)})
    .catch(err =>{console.log("error: ", err)})
}



{
    $("#btnBuscar").click(()=>{
    let ObjAdd={
            city: $("#city").val(),
            state: $("#state").val(),
            country: $("#country").val()
        }
        postCity(ObjAdd)
        })
}







