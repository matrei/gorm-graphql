package grails.test.app

import org.grails.gorm.graphql.plugin.testing.GraphQLSpec
import grails.testing.mixin.integration.Integration
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Integration
@Stepwise
class UserIntegrationSpec extends Specification implements GraphQLSpec {

    @Shared long managerId
    @Shared long subordinateId

    void "test creating a user without a profile"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userCreate(user: {
                    firstNumber: 2,
                    secondNumber: 3,
                    address: {
                        city: "Youngstown",
                        state: "OH",
                        zip: 44512
                    }
                }) {
                    id
                }
            }
        """)
        JSONObject obj = resp.json

        then:
        obj.data == null
        obj.errors.size() == 1
        obj.errors[0].message.startsWith('Validation error of type WrongType')

        when: 'The profile is provided, but missing a required field'
        resp = graphQL.graphql("""
            mutation {
                userCreate(user: {
                    firstNumber: 2,
                    secondNumber: 3,
                    profile: {
                        email: "email",
                        firstName: "First"
                    }
                    address: {
                        city: "Youngstown",
                        state: "OH",
                        zip: 44512
                    }
                }) {
                    id
                }
            }
        """)
        obj = resp.json

        then:
        obj.data == null
        obj.errors.size() == 1
        obj.errors[0].message.startsWith('Validation error of type WrongType')
    }

    void "test creating a user without an address"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userCreate(user: {
                    firstNumber: 2,
                    secondNumber: 3,
                    profile: {
                        email: "email",
                        firstName: "First",
                        lastName: "Last"
                    }
                }) {
                    id
                }
            }
        """)
        JSONObject obj = resp.json

        then:
        obj.data == null
        obj.errors.size() == 1
        obj.errors[0].message.startsWith('Validation error of type WrongType')

        when: 'The address is provided, but missing a required field'
        resp = graphQL.graphql("""
            mutation {
                userCreate(user: {
                    firstNumber: 2,
                    secondNumber: 3,
                    profile: {
                        email: "email",
                        firstName: "First",
                        lastName: "Last"
                    },
                    address: {
                        city: "Youngstown",
                        state: "OH"
                    }
                }) {
                    id
                }
            }
        """)
        obj = resp.json

        then:
        obj.data == null
        obj.errors.size() == 1
        obj.errors[0].message.startsWith('Validation error of type WrongType')
    }

    void "test creating the top level manager"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userCreate(user: {
                    firstNumber: 2,
                    secondNumber: 3,
                    profile: {
                        email: "email",
                        firstName: "First",
                        lastName: "Last"
                    }
                    address: {
                        city: "Youngstown",
                        state: "OH",
                        zip: 44512
                    }
                }) {
                    id
                    addedNumbers
                    profile {
                        email
                        firstName
                        lastName
                    }
                    address {
                        city
                        state
                        zip
                    }
                    manager {
                        id
                    }
                }
            }
        """)
        JSONObject obj = resp.json.data.userCreate
        managerId = obj.getLong("id")

        then:
        obj.id != null
        obj.addedNumbers == 5
        obj.profile.email == 'email'
        obj.profile.firstName == 'First'
        obj.profile.lastName == 'Last'
        obj.address.city == 'Youngstown'
        obj.address.state == 'OH'
        obj.address.zip == 44512
        obj.manager == null
    }

    void "create new user with manager"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userCreate(user: {
                    firstNumber: 4,
                    secondNumber: 7,
                    profile: {
                        email: "email",
                        firstName: "First",
                        lastName: "Last"
                    }
                    address: {
                        city: "Youngstown",
                        state: "OH",
                        zip: 44512
                    }
                    manager: {
                        id: ${managerId}
                    }
                }) {
                    id
                    addedNumbers
                    profile {
                        email
                        firstName
                        lastName
                    }
                    address {
                        city
                        state
                        zip
                    }
                    manager {
                        id
                    }
                }
            }
        """)
        JSONObject obj = resp.json.data.userCreate
        subordinateId = obj.getLong("id")

        then:
        obj.id != null
        obj.addedNumbers == 11
        obj.profile.email == 'email'
        obj.profile.firstName == 'First'
        obj.profile.lastName == 'Last'
        obj.address.city == 'Youngstown'
        obj.address.state == 'OH'
        obj.address.zip == 44512
        obj.manager.id == managerId
    }

    void "test updating a user"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userUpdate(id: ${subordinateId}, user: {
                    firstNumber: 5,
                    secondNumber: 7,
                    profile: {
                        email: "emailUpdated",
                        firstName: "FirstUpdated",
                        lastName: "LastUpdated"
                    }
                    address: {
                        city: "Pittsburgh",
                        state: "PA",
                        zip: 90210
                    }
                }) {
                    id
                    addedNumbers
                    profile {
                        email
                        firstName
                        lastName
                    }
                    address {
                        city
                        state
                        zip
                    }
                    manager {
                        id
                    }
                }
            }
        """)
        JSONObject obj = resp.json.data.userUpdate

        then:
        obj.id == subordinateId
        obj.addedNumbers == 12
        obj.profile.email == 'emailUpdated'
        obj.profile.firstName == 'FirstUpdated'
        obj.profile.lastName == 'LastUpdated'
        obj.address.city == 'Pittsburgh'
        obj.address.state == 'PA'
        obj.address.zip == 90210
        obj.manager.id == managerId
    }

    void "test listing users"() {
        when:
        def resp = graphQL.graphql("""
            {
                userList(sort: "id") {
                    id
                    addedNumbers
                    profile {
                        email
                        firstName
                        lastName
                    }
                    address {
                        city
                        state
                        zip
                    }
                    manager {
                        id
                        addedNumbers
                    }
                }
            }
        """)
        println resp.json
        JSONArray obj = resp.json.data.userList

        then:
        JSONObject subordinate = obj.find { it.id == subordinateId }
        JSONObject manager = obj.find { it.id == managerId }

        manager.id != null
        manager.addedNumbers == 5
        manager.profile.email == 'email'
        manager.profile.firstName == 'First'
        manager.profile.lastName == 'Last'
        manager.address.city == 'Youngstown'
        manager.address.state == 'OH'
        manager.address.zip == 44512
        manager.manager == null

        subordinate.id != null
        subordinate.addedNumbers == 12
        subordinate.profile.email == 'emailUpdated'
        subordinate.profile.firstName == 'FirstUpdated'
        subordinate.profile.lastName == 'LastUpdated'
        subordinate.address.city == 'Pittsburgh'
        subordinate.address.state == 'PA'
        subordinate.address.zip == 90210
        subordinate.manager.id != null
        subordinate.manager.addedNumbers == 5
    }

    void "test querying a single user"() {
        when:
        def resp = graphQL.graphql("""
            {
                user(id: ${subordinateId}) {
                    id
                    addedNumbers
                    profile {
                        email
                        firstName
                        lastName
                    }
                    address {
                        city
                        state
                        zip
                    }
                    manager {
                        id
                        addedNumbers
                    }
                }
            }
        """)
        JSONObject json = resp.json
        println json.toString()
        JSONObject obj = json.data.user

        then:
        obj.id == subordinateId
        obj.addedNumbers == 12
        obj.profile.email == 'emailUpdated'
        obj.profile.firstName == 'FirstUpdated'
        obj.profile.lastName == 'LastUpdated'
        obj.address.city == 'Pittsburgh'
        obj.address.state == 'PA'
        obj.address.zip == 90210
        obj.manager.id == managerId
        obj.manager.addedNumbers == 5
    }

    void "test deleting a user that is a manager of another user"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userDelete(id: ${managerId}) {
                    success
                }
            }
        """)
        JSONObject obj = resp.json.data.userDelete

        then:
        !obj.success
    }

    void "test deleting a user that is NOT a manager of another user"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userDelete(id: ${subordinateId}) {
                    success
                }
            }
        """)
        JSONObject obj = resp.json.data.userDelete

        then:
        obj.success
    }

    void "test deleting the last user is now successful"() {
        when:
        def resp = graphQL.graphql("""
            mutation {
                userDelete(id: ${managerId}) {
                    success
                }
            }
        """)
        JSONObject obj = resp.json.data.userDelete

        then:
        obj.success
    }
}
