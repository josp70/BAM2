package require TclCurl
package require snit
package require json

namespace eval ::BAM_REST {
    variable host  localhost
    variable port  9000
    variable POST_Answer ""
    variable GET_Answer  ""

    snit::type JSON_Serie {
	variable json
	
	constructor { str } {
	    set json [json::json2dict $str]
	}
	
	method getTimeSet { } {
	    dict get $json timeset
	}
	
	method getDates { } {
	    dict get $json dates
	}
	
	method getDatesAsTOL { } {
	    lmap x [$self getDates] {
		clock format [expr {$x/1000}] -format "y%Ym%md%d"
	    }
	}
	
	method getNumberOfSeries { } {
	    llength [dict get $jsonObj series]
	}
	
	method getSerieName { idx } {
	dict get [lindex [dict get $json series] $idx] name
	}
	
	method getSerieFirstDate { idx } {
	    dict get [lindex [dict get $json series] $idx] first
	}
	
	method getSerieFirstDateAsTOL { idx } {
	    clock format [expr {[$self getSerieFirstDate $idx]/1000}] -format "y%Ym%md%d"
	}
	
	method getSerieValues { idx } {
	    dict get [lindex [dict get $json series] $idx] values
	}

	method getSerieValuesAsTOL { idx } {
	    set argValues [join [$self getSerieValues $idx] ,]
	    return "SetOfReal($argValues)"
	}
    }
    
    proc SetServerInfo { args } {
        array set opts {
            -host localhost
            -port 9000
        }
        array set opts $args
        variable host  $opts(-host)
        variable port  $opts(-port)
    }
    
    proc EpochToDate { clockTimeMilliSec } {
	clock format [expr {$clockTimeMilliSec/1000}] -format "%Y-%m-%d" 
    }

    proc DateToEpoch { strDate } {
	expr {[clock scan $strDate -format "%Y-%m-%d"]*1000}
    }
    
    proc POST_Retrieve { id } {
	variable POST_Answer
	
	set POST_Answer $id
    }

    proc GET_Retrieve { data } {
	variable  GET_Answer
    
	append GET_Answer $data
    }

    proc POST_Data { args } {
	variable POST_Answer
	variable host
	variable port
	
	array set opts {
	    -verbose 0
	    -bdt ""
	    -json ""
	}
	
	array set opts $args
	set curlHandle [curl::init]
	$curlHandle configure -verbose $opts(-verbose) \
	    -url http://${host}:${port}/forecast \
		-writeproc ::BAM_REST::POST_Retrieve

	if {$opts(-json) ne ""} {
	    $curlHandle configure \
		-httpheader [list "Content-Type: application/json" ] \
		-postfields $opts(-json)
	} elseif {$opts(-bdt) ne ""} {
	    #$curlHandle configure -httppost [list name dataframe file $opts(-bdt)]
	    $curlHandle configure \
		-httpheader [list "Content-type: text/plain"] \
		-postfields $opts(-bdt)
	} else {
	    error "missing option: -json or -bdt must be provided"
	}
	set status [$curlHandle perform]
	$curlHandle cleanup
	if {$status} {
	    return {}
	} else {
	    return $POST_Answer
	}
    }

    proc GET_Route { route id args } {
	variable GET_Answer
	variable host
	variable port

	array set opts {
	    -verbose 0
	    -tofile {}
	    -wait 0
	}
	array set opts $args
	set curlHandle [curl::init]
	set waitAcc 0
	while {$waitAcc <= $opts(-wait)} {
	    $curlHandle configure -verbose $opts(-verbose) \
		-url http://${host}:${port}/${route}/${id}
	    if {$opts(-tofile) ne {}} {
		$curlHandle configure -file $opts(-tofile)
		set GET_Answer $opts(-tofile)
	    } else {
		set GET_Answer {}
		$curlHandle configure -writeproc ::BAM_REST::GET_Retrieve
	    }
	    set status [$curlHandle perform]
	    if {$status} {
		$curlHandle cleanup
		return [list $status "curl failed"]
	    }
	    set rcode [$curlHandle getinfo responsecode]
	    switch $rcode {
		200 {
		    $curlHandle cleanup
		    return [list 0 $GET_Answer]
		}
		204 {
		    set GET_Answer [list 204 "No content available"]
		}
		404 {
		    $curlHandle cleanup
		    return [list 404 "resource not found"]
		}
		default {
		    $curlHandle cleanup
		    return [list [expr {1000+$rcode}] "Unknown error"]
		}
	    }
	    after 200 {set ::BAM_REST::__wait__ 1}
	    vwait ::BAM_REST::__wait__
	    incr waitAcc 100
	}
	$curlHandle cleanup
	return [list 804 "timeout, no content available"]
    }

    proc GET_Log { id args } {
	return [GET_Route logs $id {*}$args]
    }

    proc GET_ForecastWithCode { id args } {
	return [GET_Route forecast $id {*}$args]
    }
    
    proc GET_Forecast { id args } {
	set result [GET_ForecastWithCode $id {*}$args]
	if {[lindex $result 0]} {
	    return "ERROR [lindex $result 1]"
	} else {
	    return [lindex $result 1]
	}
    }
}
