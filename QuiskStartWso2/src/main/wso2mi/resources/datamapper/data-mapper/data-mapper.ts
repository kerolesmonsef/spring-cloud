import * as dmUtils from "./dm-utils";
declare var DM_PROPERTIES: any;

/*
* title : "root",
* inputType : "JSON",
*/
interface Root {
    users: {
        id: number
        fname: string
        lname: string
        createdAt: string
    }[]
}

/*
* title : "root",
* outputType : "JSON",
*/
interface OutputRoot {
    users: {
        id: number
        full_name: string
    }[]
}



/**
 * functionName : map_S_root_S_root
 * inputVariable : inputroot
*/
export function mapFunction(input: Root): OutputRoot {
    return {
        users: input.users.map(u => ({
            id: u.id,
            full_name: u.fname + " " + u.lname
        }))
    };
}

